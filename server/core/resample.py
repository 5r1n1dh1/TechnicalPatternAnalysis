"""
Resampling utilities for converting daily OHLCV data to higher timeframes.
"""
import pandas as pd
from typing import Dict, Any


def resample_daily_to_interval(df: pd.DataFrame, interval: str) -> pd.DataFrame:
    """
    Resample daily OHLCV data to the specified interval.
    
    Args:
        df: DataFrame with columns ['date', 'open', 'high', 'low', 'close', 'volume']
            where 'date' is datetime-like and set as index
        interval: Target interval ('1w', '1M')
        
    Returns:
        DataFrame with resampled OHLCV data
    """
    # Ensure date is index
    if 'date' in df.columns:
        df = df.set_index('date')
    
    # Resample based on interval
    if interval == '1w':
        # Weekly resample (ending on Friday)
        resampled = df.resample('W-FRI').agg({
            'open': 'first',
            'high': 'max',
            'low': 'min',
            'close': 'last',
            'volume': 'sum'
        })
    elif interval == '1M':
        # Monthly resample (ending on last day of month)
        resampled = df.resample('M').agg({
            'open': 'first',
            'high': 'max',
            'low': 'min',
            'close': 'last',
            'volume': 'sum'
        })
    else:
        # For other intervals, return as-is (should not be called for these)
        resampled = df
    
    # Reset index to have date as column
    resampled = resampled.reset_index()
    resampled = resampled.rename(columns={'index': 'date'})
    
    # Remove rows with NaN (incomplete periods)
    resampled = resampled.dropna()
    
    return resampled


def resample_daily_to_range(df: pd.DataFrame, range_str: str) -> pd.DataFrame:
    """
    Resample daily OHLCV data to the specified range (for very long ranges).
    
    Args:
        df: DataFrame with columns ['date', 'open', 'high', 'low', 'close', 'volume']
            where 'date' is datetime-like and set as index
        range_str: Target range ('1Y', '2Y', '5Y', '10Y')
        
    Returns:
        DataFrame with resampled OHLCV data (annual data)
    """
    # Ensure date is index
    if 'date' in df.columns:
        df = df.set_index('date')
    
    # For ranges >= 1Y, we resample to yearly data
    if range_str in ['1Y', '2Y', '5Y', '10Y']:
        # Yearly resample (ending on Dec 31)
        resampled = df.resample('Y').agg({
            'open': 'first',
            'high': 'max',
            'low': 'min',
            'close': 'last',
            'volume': 'sum'
        })
    else:
        # For shorter ranges, return as-is (should not be called for these)
        resampled = df
    
    # Reset index to have date as column
    resampled = resampled.reset_index()
    resampled = resampled.rename(columns={'index': 'date'})
    
    # Remove rows with NaN (incomplete periods)
    resampled = resampled.dropna()
    
    return resampled