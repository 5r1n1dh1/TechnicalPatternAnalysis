# Backfill script to load real universe history (Nifty 500 + NSEI benchmark)

import os
import pandas as pd
import yfinance as yf
from datetime import datetime, timedelta
from pathlib import Path

# Real universe source
SYMBOLS_FILE = Path(__file__).parent / ".." / "server" / "data" / "symbols.json"

def load_universe():
    """Load NSE Nifty-500 symbols plus ^NSEI benchmark from server/data/symbols.json."""
    if not SYMBOLS_FILE.exists():
        raise RuntimeError(f"Symbols file not found: {SYMBOLS_FILE}")
    with open(SYMBOLS_FILE) as f:
        import json
        symbols = json.load(f)
    # Expect list of {"symbol": str, "name": str, "sector": str}
    tickers = [s["symbol"] for s in symbols]
    # Append NIFTY 50 benchmark symbol (^NSEI) for relStrength calculations
    if "^NSEI" not in tickers:
        tickers.append("^NSEI")
    return tickers


def backfill_data(limit: int = None, validate_sample: bool = True):
    """Fetch 10y adjusted daily bars for real universe, write to history.parquet.
    
    Args:
        limit: optional integer to cap symbols for smoke runs (dev/test).
        validate_sample: if True, after download verify the expected schema columns exist.
    
    Schema (eod-compatible):
        SC_CODE: uppercase symbol (e.g., "RELIANCE", "^NSEI")
        Date: ISO-8601 "YYYY-MM-DD"
        open, high, low, close, volume: floats
        adjOpen, adjHigh, adjLow, adjClose: same as open/high/low/close (duplicates for backward compat)
    """
    import numpy as np
    
    all_data = []
    failures = []
    
    try:
        tickers = load_universe()
        print(f"Backfill universe: {len(tickers)} tickers (Nifty-500 + ^NSEI)")
    except Exception as e:
        print(f"Failed to load universe: {e}")
        raise
    
    if limit:
        tickers = tickers[:limit]
        print(f"Smoke mode: limited to {len(tickers)} symbols")
    
    start_date = (datetime.now() - pd.Timedelta(days=3650)).strftime("%Y-%m-%d")
    end_date = datetime.now().strftime("%Y-%m-%d")
    
    for i, symbol in enumerate(tickers):
        try:
            # Fetch 10y daily adjusted bars; use `auto_adjust=True` to avoid column mismatch.
            # yfinance returns a MultiIndex DataFrame for a single symbol — flatten.
            data = yf.download(symbol, start=start_date, end=end_date, auto_adjust=True, threads=False, progress=False)
            if data.empty:
                raise ValueError(f"No data returned for {symbol}")
            
            # Flatten MultiIndex (Price, Ticker) if present — safe for all symbols.
            if getattr(data.columns, "nlevels", 1) > 1:
                data.columns = data.columns.get_level_values(0)
            
            # Ensure column names are lowercase and standard (use yahoo naming convention)
            # Rename known aliases: Open/High/Low/Close/Volume
            rename = {}
            for k in ["Open", "High", "Low", "Close", "Volume"]:
                if k in data.columns and k.lower() not in data.columns:
                    rename[k] = k.lower()
            data = data.rename(columns=rename)
            
            # Normalize all numeric columns to float64
            for col in ["open", "high", "low", "close", "volume", "adjopen", "adjhigh", "adjlow", "adjclose"]:
                if col in data.columns:
                    data[col] = pd.to_numeric(data[col], errors="coerce")
            
            # Build rows in the expected eod/pyarrow schema
            # Use the symbol as-is for SC_CODE (uppercase)
            sc_code = str(symbol).strip().upper()
            
            # Ensure we have valid rows before producing them
            if data.empty or data[["open", "high", "low", "close", "volume"]].isna().all().any():
                raise ValueError("Downloaded data is empty or all NaN")
            
            # Each day becomes a row
            for date, row in data.iterrows():
                date_str = date.strftime("%Y-%m-%d")
                all_data.append({
                    "SC_CODE": sc_code,
                    "Date": date_str,
                    "open": float(row["open"]),
                    "high": float(row["high"]),
                    "low": float(row["low"]),
                    "close": float(row["close"]),
                    "volume": int(float(row["volume"])) if not pd.isna(row["volume"]) else 0,
                    "adjOpen": float(row.get("adjopen", row["open"])),
                    "adjHigh": float(row.get("adjhigh", row["high"])),
                    "adjLow": float(row.get("adjlow", row["low"])),
                    "adjClose": float(row.get("adjclose", row["close"])),
                })
            
            if (i + 1) % 50 == 0:
                print(f"Processed {i + 1}/{len(tickers)} symbols")
                
        except Exception as e:
            failures.append((symbol, str(e)))
            print(f"Error downloading data for {symbol}: {str(e)}")
            continue
    
    if not all_data:
        raise RuntimeError("Backfill produced no rows")
    
    # Convert to DataFrame and order columns for eod compatibility
    df = pd.DataFrame(all_data)
    # Ensure required columns present
    required = ["SC_CODE", "Date", "open", "high", "low", "close", "volume", "adjOpen", "adjHigh", "adjLow", "adjClose"]
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise RuntimeError(f"Missing columns after backfill: {missing}")
    
    # Reorder columns for easier diff + backwards compat
    df = df[required]
    
    # If a history file already exists, merge by unique (SC_CODE, Date) keeping latest.
    out_path = Path(__file__).parent / ".." / "history.parquet"
    if out_path.exists():
        try:
            existing = pd.read_parquet(out_path)
            # Normalize column names for merge
            if "date" in existing.columns:
                existing = existing.rename(columns={"date": "Date"})
            # Concat and deduplicate
            combined = pd.concat([existing, df], ignore_index=True)
            # Keep last row per (SC_CODE, Date)
            combined = combined.drop_duplicates(subset=["SC_CODE", "Date"], keep="last")
            df = combined
            print(f"Merged with existing history: {len(existing)} → {len(df)} total rows")
        except Exception as e:
            print(f"Warning: could not merge existing history ({e}); overwriting")
    
    # Write to parquet (pyarrow via pandas; ensure `pyarrow` is installed per requirements)
    try:
        df.to_parquet(out_path, index=False)
    except Exception as e:
        raise RuntimeError(f"Failed to write parquet file {out_path}: {e}")
    
    # Print summary
    print(f"Backfilled data saved to {out_path}")
    print(f"Total rows: {len(df)}")
    print(f"Unique symbols: {df['SC_CODE'].nunique()}")
    print(f"Date range: {df['Date'].min()} to {df['Date'].max()}")
    
    if failures:
        print(f"\nFailures ({len(failures)}):")
        for symbol, err in failures[:10]:
            print(f"  {symbol}: {err}")
        if len(failures) > 10:
            print(f"  ... and {len(failures) - 10} more")
        # Fail fatally only when every symbol failed (data pipeline is broken).
        if len(failures) == len(tickers):
            raise RuntimeError(f"All symbols failed: {len(failures)} / {len(tickers)}")
    
    # Schema validation (optional, cheap)
    if validate_sample:
        sample = df.head(2)
        for col in required:
            dtype = sample[col].dtype
            print(f"  {col}: {dtype}")
    
    return df


if __name__ == "__main__":
    import sys
    
    # Simple CLI for smoke runs and production backfills
    limit = None
    if "--limit" in sys.argv:
        idx = sys.argv.index("--limit")
        if idx + 1 < len(sys.argv):
            try:
                limit = int(sys.argv[idx + 1])
            except ValueError:
                print(f"Invalid limit {sys.argv[idx + 1]}; ignoring")
    
    try:
        df = backfill_data(limit=limit)
        print("Backfill completed successfully.")
    except Exception as e:
        print(f"Backfill failed: {e}")
        raise