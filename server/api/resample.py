"""
Resampling endpoint for chart data.
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
import pandas as pd

from core.resample import resample_daily_to_interval, resample_daily_to_range
from core.constants import validate_combo

router = APIRouter()

class ResampleRequest(BaseModel):
    symbol: str
    interval: str
    range: str

class ResampleResponse(BaseModel):
    symbol: str
    interval: str
    tz: str
    source: str
    candles: list[dict]

@router.get("/api/resample", response_model=ResampleResponse)
def get_resampled_data(
    request: ResampleRequest,
    interval: str,
    range: str,
):
    # Validate interval and range
    validate_combo(interval, range)
    
    # Determine source
    # For now, we'll use yfinance as primary source
    import yfinance as yf
    symbol = request.symbol
    # Add .NS suffix for NSE symbols
    if not symbol.endswith('.NS'):
        symbol += '.NS'
    
    try:
        # Fetch data from Yahoo Finance
        ticker = yf.Ticker(symbol)
        # Get historical data (daily)
        data = ticker.history(period="max", interval="1d")
        # Ensure we have data
        if data.empty:
            raise HTTPException(status_code=503, detail="No data available")
        
        # Convert to DataFrame
        data = data.reset_index()
        data = data.rename(columns={
            'Date': 'date',
            'Open': 'open',
            'High': 'high',
            'Low': 'low',
            'Close': 'close',
            'Volume': 'volume'
        })
        # Ensure required columns exist
        required_cols = ['date', 'open', 'high', 'low', 'close', 'volume']
        for col in required_cols:
            if col not in data.columns:
                raise HTTPException(status_code=500, detail=f"Missing column: {col}")
        
        # Convert to DataFrame with proper types
        data = data[['date', 'open', 'high', 'low', 'close', 'volume']].copy()
        data['date'] = pd.to_datetime(data['date']).dt.tz_localize('UTC').dt.tz_convert('Asia/Kolkata')
        data = data.rename(columns={
            'date': 'date',
            'open': 'open',
            'high': 'high',
            'low': 'low',
            'close': 'close',
            'volume': 'volume'
        })
        
        # Resample according to interval and range
        if interval in ['5m', '15m', '30m', '1h']:
            # These are intraday - use daily data only
            data = data.resample('1d').agg({
                'open': 'first',
                'high': 'max',
                'low': 'min',
                'close': 'last',
                'volume': 'sum'
            })
        elif interval in ['1d', '1w', '1M']:
            # Use resample_daily_to_interval
            resampled = resample_daily_to_interval(data, interval)
        else:
            # For 1d, 1w, 1M, we use daily data directly
            resampled = data
        
        # Apply range resampling if needed
        if range != '1D':
            resampled = resample_daily_to_range(resampled, range)
        
        # Prepare response
        response = {
            "symbol": symbol,
            "interval": interval,
            "tz": "Asia/Kolkata",
            "source": "yfinance",
            "candles": resampled.to_dict(orient='records')
        }
        
        return response