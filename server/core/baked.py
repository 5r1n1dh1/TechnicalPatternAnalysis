"""Read the baked EOD candle parquet first.

Vercel's datacenter IPs get rate-limited by Yahoo, so endpoints read this
committed dataset before attempting live yfinance.

This module is imported by serverless functions: keep the import cheap, import
pandas lazily inside functions, and never raise at import time.
"""

from __future__ import annotations

from pathlib import Path

_PARQUET = Path(__file__).resolve().parents[1] / "data" / "candles.parquet"

# range -> trailing rows (trading days) to keep; "10Y" -> 0 means "keep all".
_RANGE_ROWS = {
    "1D": 1,
    "5D": 5,
    "1M": 22,
    "3M": 66,
    "6M": 132,
    "1Y": 252,
    "2Y": 504,
    "5Y": 1260,
    "10Y": 0,
}

# Cache of the loaded parquet keyed by (mtime_ns, size) so repeat calls are cheap.
_CACHE: dict = {}


def _read_parquet(path: Path):
    """Read a parquet file, trying the common engines. Returns None on failure."""
    import pandas as pd

    for kwargs in ({"engine": "pyarrow"}, {"engine": "fastparquet"}, {}):
        try:
            return pd.read_parquet(path, **kwargs)
        except Exception:  # noqa: BLE001 - engine may be missing/unable to read
            continue
    return None


def _get_frame(path: Path):
    """Return the cached parquet frame for `path`, re-reading when it changes."""
    try:
        stat = path.stat()
    except OSError:
        return None
    key = (stat.st_mtime_ns, stat.st_size)
    if key in _CACHE:
        return _CACHE[key]
    df = _read_parquet(path)
    if df is None or df.empty:
        return None
    _CACHE.clear()
    _CACHE[key] = df
    return df


def _bare(symbol: str) -> str:
    s = str(symbol).strip().upper()
    for suf in (".NS", ".BO"):
        if s.endswith(suf):
            return s[: -len(suf)]
    return s


def load_baked(symbol: str, range_str: str = "1Y"):
    """Return a `t,o,h,l,c,v` DataFrame (sorted by `t`) or None when unavailable.

    Returns None if the file/symbol is missing, unreadable, or empty.
    """
    try:
        if not symbol:
            return None
        if not _PARQUET.exists():
            return None
        df = _get_frame(_PARQUET)
        if df is None:
            return None
        if "symbol" not in df.columns or "t" not in df.columns:
            return None

        sub = df[df["symbol"] == _bare(symbol)]
        if sub.empty:
            return None

        wanted = [c for c in ("t", "o", "h", "l", "c", "v") if c in sub.columns]
        if len(wanted) < 6:
            return None

        import pandas as pd

        sub = sub[wanted].copy()
        sub["t"] = pd.to_datetime(sub["t"])
        sub = sub.sort_values("t")

        rows = _RANGE_ROWS.get(str(range_str).upper(), 252)
        if rows:
            sub = sub.tail(rows)
        return sub.reset_index(drop=True)
    except Exception:  # noqa: BLE001 - never let the baked read break an endpoint
        return None
