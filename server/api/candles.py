"""Candles endpoint for stock historical data."""
from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Optional
from enum import Enum
from datetime import datetime
from zoneinfo import ZoneInfo
import os
from pathlib import Path

from server.core.constants import validate_combo, INTERVALS, RANGES

router = APIRouter()

# In-memory cache with TTL 12h
_cache = {}
_CACHE_TTL = 12 * 60 * 60  # 12 hours in seconds

class Interval(str, Enum):
    interval_5m = "5m"
    interval_15m = "15m"
    interval_30m = "30m"
    interval_1h = "1h"
    interval_1d = "1d"
    interval_1w = "1w"
    interval_1M = "1M"


class Range(str, Enum):
    range_1D = "1D"
    range_5D = "5D"
    range_1M = "1M"
    range_3M = "3M"
    range_6M = "6M"
    range_1Y = "1Y"
    range_2Y = "2Y"
    range_5Y = "5Y"
    range_10Y = "10Y"


class Candle(BaseModel):
    t: str
    o: float
    h: float
    l: float
    c: float
    v: float


class CandleResponse(BaseModel):
    symbol: str
    interval: str
    range: str
    tz: str = "Asia/Kolkata"
    source: str
    candles: List[Candle]


@router.get("/candles", response_model=CandleResponse)
def get_candles(
    symbol: str = Query(..., description="Stock symbol"),
    interval: Interval = Query(..., description="Time interval"),
    range: Range = Query(..., description="Data range"),
):
    """Get historical OHLCV candles."""
    interval_val = interval.value
    range_val = range.value

    # Validate combo
    validate_combo(interval_val, range_val)

    # Check in-memory cache
    cache_key = f"{symbol}_{interval_val}_{range_val}"
    if cache_key in _cache:
        cached_data, cached_time = _cache[cache_key]
        if (datetime.now().timestamp() - cached_time) < _CACHE_TTL:
            return cached_data

    # Try Yahoo Finance primary
    try:
        df = _fetch_from_yahoo(symbol, interval_val, range_val)
        if df is not None:
            response = _process_candles(df, symbol, interval_val, range_val, source="yahoo")
            _cache[cache_key] = (response, datetime.now().timestamp())
            return response
    except Exception:
        pass  # Guarded fallback

    # Try Kaggle fallback
    try:
        df = _fetch_from_kaggle(symbol)
        if df is not None:
            response = _process_candles(df, symbol, interval_val, range_val, source="kaggle")
            _cache[cache_key] = (response, datetime.now().timestamp())
            return response
    except Exception:
        pass  # Guarded fallback

    raise HTTPException(status_code=503, detail="Data source unavailable")


def _process_candles(df, symbol: str, interval: str, range_val: str, source: str) -> CandleResponse:
    """Process and format candle data."""
    import pandas as pd

    # Convert timezone to +05:30 (Asia/Kolkata)
    ist = ZoneInfo('Asia/Kolkata')
    utc = ZoneInfo('UTC')

    candles = []
    for _, row in df.iterrows():
        t = row['t']
        if hasattr(t, 'tzinfo') and t.tzinfo is not None:
            t = t.astimezone(ist)
        else:
            # Assume naive datetime in UTC
            import pandas as pd
            t = pd.to_datetime(t)
            if t.tzinfo is None:
                t = t.replace(tzinfo=utc).astimezone(ist)

        # Format with +05:30
        candles.append(Candle(
            t=t.strftime('%Y-%m-%dT%H:%M:%S+05:30'),
            o=float(row['o']),
            h=float(row['h']),
            l=float(row['l']),
            c=float(row['c']),
            v=float(row['v']),
        ))

    return CandleResponse(
        symbol=symbol,
        interval=interval,
        range=range_val,
        tz="Asia/Kolkata",
        source=source,
        candles=candles,
    )


def _fetch_from_yahoo(symbol: str, interval: str, range_val: str):
    """Fetch from Yahoo Finance - primary source."""
    try:
        import yfinance as yf
        import pandas as pd

        ticker = yf.Ticker(symbol + ".NS" if not symbol.endswith(('.NS', '.BO')) else symbol)
        # Map range values for yfinance
        range_map = {
            "1D": "1d", "5D": "5d", "1M": "1mo", "3M": "3mo", "6M": "6mo",
            "1Y": "1y", "2Y": "2y", "5Y": "5y", "10Y": "max"
        }
        period = range_map.get(range_val, "1y")
        # Map interval values for yfinance
        interval_map = {
            "5m": "5m", "15m": "15m", "30m": "30m", "1h": "60m",
            "1d": "1d", "1w": "1wk", "1M": "1mo"
        }
        yf_interval = interval_map.get(interval, "1d")

        df = ticker.history(period=period, interval=yf_interval)
        if df.empty:
            return None

        df = df.reset_index()
        # Handle different column names
        if 'Date' in df.columns:
            df = df.rename(columns={'Date': 't'})
        elif 'Datetime' in df.columns:
            df = df.rename(columns={'Datetime': 't'})
        elif 'time' in df.columns:
            df = df.rename(columns={'time': 't'})

        df = df.rename(columns={
            'Open': 'o', 'High': 'h', 'Low': 'l', 'Close': 'c', 'Volume': 'v'
        })
        df = df[['t', 'o', 'h', 'l', 'c', 'v']].copy()
        return df
    except Exception:
        return None


def _fetch_from_kaggle(symbol: str):
    """Fetch from Kaggle dataset - fallback."""
    try:
        # Try to load from local kaggle cache if it exists
        kaggle_file = Path(__file__).resolve().parents[1] / "data" / f"{symbol}.parquet"
        if kaggle_file.exists():
            import pandas as pd
            df = pd.read_parquet(kaggle_file)
            if df is not None and not df.empty:
                # Ensure columns are named correctly
                if 'Open' in df.columns:
                    df = df.rename(columns={'Open': 'o', 'High': 'h', 'Low': 'l', 'Close': 'c', 'Volume': 'v'})
                if 'Date' in df.columns:
                    df = df.rename(columns={'Date': 't'})
                return df[['t', 'o', 'h', 'l', 'c', 'v']].copy()
        return None
    except Exception:
        return None
