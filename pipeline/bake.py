"""Bake daily OHLCV bars into server/data/candles.parquet (LONG format).

Vercel's datacenter IPs get rate-limited by Yahoo, so the server reads this
committed file first and only falls back to live yfinance.

Usage:
    python -m pipeline.bake [--limit N] [--period 1y]

Output columns (exact): symbol, t, o, h, l, c, v
    symbol = bare NSE symbol (no .NS/.BO suffix)
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SYMBOLS_JSON = REPO_ROOT / "server" / "data" / "symbols.json"
OUT_PARQUET = REPO_ROOT / "server" / "data" / "candles.parquet"

PRICE_COLS = ("Open", "High", "Low", "Close", "Volume")
CHUNK = 40


def _bare(symbol: str) -> str:
    """Strip any .NS/.BO suffix and normalise to upper case."""
    s = str(symbol).strip().upper()
    for suf in (".NS", ".BO"):
        if s.endswith(suf):
            return s[: -len(suf)]
    return s


def _load_symbols(limit: int = 0):
    """Read server/data/symbols.json -> ordered, de-duplicated bare symbols."""
    raw = json.loads(SYMBOLS_JSON.read_text(encoding="utf-8"))
    if isinstance(raw, dict):
        raw = raw.get("symbols", [])
    out, seen = [], set()
    for item in raw:
        sym = _bare(item.get("symbol", "")) if isinstance(item, dict) else _bare(item)
        if sym and sym not in seen:
            seen.add(sym)
            out.append(sym)
    if limit and limit > 0:
        out = out[:limit]
    return out


def _flatten(raw, ticker: str):
    """Return a per-ticker frame with Open/High/Low/Close/Volume columns or None.

    Handles yfinance MultiIndex layouts (Price, Ticker) or (Ticker, Price).
    """
    if raw is None or len(raw) == 0:
        return None
    cols = raw.columns
    if getattr(cols, "nlevels", 1) > 1:
        level0 = [str(v) for v in cols.get_level_values(0)]
        price_level = 0 if any(p in level0 for p in PRICE_COLS) else 1
        ticker_level = 1 - price_level
        tickers_at_level = {str(v) for v in cols.get_level_values(ticker_level)}
        if ticker not in tickers_at_level:
            return None
        sub = raw.xs(ticker, axis=1, level=ticker_level)
    else:
        # Single ticker downloaded alone: columns are already price names.
        sub = raw
    sub = sub[[c for c in PRICE_COLS if c in sub.columns]]
    if not {"Open", "High", "Low", "Close"}.issubset(sub.columns):
        return None
    return sub


def _to_long(raw, bare: str, yf_ticker: str):
    import pandas as pd

    sub = _flatten(raw, yf_ticker)
    if sub is None:
        return None
    sub = sub.dropna(subset=["Open", "High", "Low", "Close"])
    if sub.empty:
        return None
    if "Volume" in sub.columns:
        vol = pd.to_numeric(sub["Volume"], errors="coerce").fillna(0.0).to_numpy(dtype="float64")
    else:
        vol = [0.0] * len(sub)
    return pd.DataFrame(
        {
            "symbol": bare,
            "t": pd.to_datetime(sub.index),
            "o": sub["Open"].to_numpy(dtype="float64"),
            "h": sub["High"].to_numpy(dtype="float64"),
            "l": sub["Low"].to_numpy(dtype="float64"),
            "c": sub["Close"].to_numpy(dtype="float64"),
            "v": vol,
        }
    )


def main(argv=None) -> int:
    import pandas as pd
    import yfinance as yf

    parser = argparse.ArgumentParser(description="Bake EOD candles into a parquet file.")
    parser.add_argument("--limit", type=int, default=0, help="max symbols (0 = all)")
    parser.add_argument("--period", default="1y", help="yfinance period (default 1y)")
    args = parser.parse_args(argv)

    symbols = _load_symbols(args.limit)
    if not symbols:
        print("WARN no symbols loaded", file=sys.stderr)

    frames, ok = [], 0
    for start in range(0, len(symbols), CHUNK):
        chunk = symbols[start : start + CHUNK]
        tickers = [s + ".NS" for s in chunk]
        try:
            raw = yf.download(
                tickers,
                period=args.period,
                interval="1d",
                auto_adjust=False,
                progress=False,
                group_by="ticker",
                threads=True,
            )
        except Exception as exc:  # noqa: BLE001 - a bad chunk must not abort the run
            print(f"WARN chunk starting at {start} failed: {exc}", file=sys.stderr)
            continue
        if raw is None or len(raw) == 0:
            print(f"WARN chunk starting at {start} returned no data", file=sys.stderr)
            continue
        for bare, yf_ticker in zip(chunk, tickers):
            try:
                df = _to_long(raw, bare, yf_ticker)
            except Exception as exc:  # noqa: BLE001
                print(f"WARN {bare}: {exc}", file=sys.stderr)
                continue
            if df is not None and not df.empty:
                frames.append(df)
                ok += 1

    if ok == 0:
        print("symbols_ok=0 rows=0")
        print("ERROR: no symbols succeeded; leaving any existing parquet untouched", file=sys.stderr)
        return 1

    out = pd.concat(frames, ignore_index=True)
    out["symbol"] = out["symbol"].astype(str)
    out["t"] = pd.to_datetime(out["t"])
    for col in ("o", "h", "l", "c", "v"):
        out[col] = pd.to_numeric(out[col], errors="coerce")
    out = out.dropna(subset=["o", "h", "l", "c"])
    # Plain `object` strings (not Arrow large_string) + parquet format 1.0 keep
    # the artifact readable by the server runtime (fastparquet) as well as pyarrow.
    out["symbol"] = out["symbol"].astype(object)
    out = (
        out[["symbol", "t", "o", "h", "l", "c", "v"]]
        .sort_values(["symbol", "t"])
        .reset_index(drop=True)
    )

    OUT_PARQUET.parent.mkdir(parents=True, exist_ok=True)
    try:
        import pyarrow as pa
        import pyarrow.parquet as pq

        pq.write_table(
            pa.Table.from_pandas(out, preserve_index=False),
            OUT_PARQUET,
            version="1.0",
            data_page_version="1.0",
            compression="snappy",
        )
    except Exception:
        out.to_parquet(OUT_PARQUET, index=False, version="1.0")

    print(f"symbols_ok={ok} rows={len(out)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
