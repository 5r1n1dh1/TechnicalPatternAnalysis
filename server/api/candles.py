"""Candles endpoint for stock historical data.

Provides EOD (End of Day) candlestick data with caching and fallback.
"""

from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Optional
import os
import pandas as pd
import yfinance as yf
from datetime import datetime, timedelta
from enum import Enum
import pytz

class Interval(str, Enum):
    """Time interval options."""
    1m = "1m"
    5m = "5m"
    15m = "15m"
    30m = "30m"
    60m = "60m"
    90m = "90m"
    1d = "1d"
    1w = "1w"

class Range(str, Enum):
    """Data range options."""
    1d = "1d"
    1mo = "1mo"
    3mo = "3mo"
    6mo = "6mo"
    1y = "1y"
    2y = "2y"
    5y = "5y"
    max = "max"

router = APIRouter()

class CandleQuery(BaseModel):
    """Query parameters for candles endpoint."""
    symbol: str
    interval: Interval
    range: Range

class CandleResponse(BaseModel):
    """Candlestick data response model."""
    symbol: str
    interval: str
    tz: str
    source: str
    candles: List[Dict[str, float]]

@router.get("/candles", response_model=CandleResponse)
def get_candles(query: CandleQuery):
    """Get historical candlestick data with caching and fallback."""
    # Validate interval and range combination
    if not validate_combo(query.interval, query.range):
        raise HTTPException(status_code=400, detail="Invalid interval and range combination")
    
    symbol = query.symbol
    interval = query.interval.value
    range_val = query.range.value
    
    # Construct cache file path
    cache_file = f"server/data/cached_candles/{symbol}_{interval}_{range_val}.parquet"
    
    # Check if cache exists and is within TTL (12 hours)
    if os.path.exists(cache_file):
        try:
            df = pd.read_parquet(cache_file)
            # Check if data is within 12 hours
            if (datetime.now(pytz.timezone('UTC')) - df['t'].max()).total_seconds() < 43200:
                return _process_candles(df, symbol, interval, range_val)
        except Exception:
            pass
    
    # Try to fetch from Yahoo Finance
    try:
        df = _fetch_from_yahoo(symbol, interval, range_val)
        if df is not None:
            # Save to cache
            df.to_parquet(cache_file)
            return _process_candles(df, symbol, interval, range_val)
    except Exception:
        pass
    
    # Fallback to Kaggle data if available
    try:
        df = _fetch_from_kaggle(symbol)
        if df is not None:
            # Save to cache
            df.to_parquet(cache_file)
            return _process_candles(df, symbol, interval, range_val)
    except Exception:
        pass
    
    # If all else fails
    raise HTTPException(status_code=404, detail="Candle data not found")

def _process_candles(df: pd.DataFrame, symbol: str, interval: str, range_val: str) -> CandleResponse:
    """Process and format candle data."""
    # Convert to UTC then to +05:30 timezone
    df['t'] = pd.to_datetime(df['t']).dt.tz_localize('UTC').dt.tz_convert('Asia/Kolkata')
    
    # Convert to ISO 8601 format with timezone
    candles = []
    for _, row in df.iterrows():
        candles.append({
            "t": row['t'].isoformat() + "+05:30",
            "o": float(row['Open']),
            "h": float(row['High']),
            "l": float(row['Low']),
            "c": float(row['Close']),
            "v": float(row['Volume'])
        })
    
    return CandleResponse(
        symbol=symbol,
        interval=interval,
        tz="Asia/Kolkata",
        source="yahoo" if df is not None else "kaggle",
        candles=candles
    )

def _fetch_from_yahoo(symbol: str, interval: str, range_val: str) -> Optional[pd.DataFrame]:
    """Fetch data from Yahoo Finance."""
    try:
        ticker = yf.Ticker(symbol)
        df = ticker.history(period=range_val, interval=interval)
        if df.empty:
            return None
        # Convert to required format
        df = df[['Open', 'High', 'Low', 'Close', 'Volume']]
        df = df.reset_index()
        df.columns = ['t', 'o', 'h', 'l', 'c', 'v']
        return df
    except Exception:
        return None

def _fetch_from_kaggle(symbol: str) -> Optional[pd.DataFrame]:
    """Fetch data from Kaggle dataset (fallback)."""
    try:
        # This is a placeholder - actual implementation would load from Kaggle parquet
        # For now, we'll simulate some data
        return None
    except Exception:
        return None

def validate_combo(interval: str, range: str) -> bool:
    """Validate interval and range combination."""
    # Implement validation matrix from design/plan.md (C23)
    # Example validation:
    valid_combinations = {
        "1m": ["1d"],
        "5m": ["1d", "1mo"],
        "15m": ["1mo"],
        "30m": ["3mo"],
        "60m": ["6mo"],
        "90m": ["1y"],
        "1d": ["1d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "max"],
        "1w": ["1y", "2y", "5y", "max"]
    }
    
    return range in valid_combinations.get(interval, [])
    
# Add more validation as needed based on design/plan.md