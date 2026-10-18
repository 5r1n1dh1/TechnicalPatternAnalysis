# EOD data processing

import os
import pandas as pd
import numpy as np
import requests
import yfinance as yf
from datetime import datetime
from dateutil.relativedelta import relativedelta
import json

# NSE calendar JSON - holidays list
with open('server/core/constants.py') as f:
    content = f.read()
    # Extract holidays from constants.py
    # This is a placeholder - in reality we'd extract the holidays list
    holidays = []  # In a real implementation, this would be loaded from constants.py

def fetch_bhavcopy(date: str):
    """Fetch NSE bhavcopy CSV via requests, handles cookie handshake."""
    # Validate date format
    try:
        datetime.strptime(date, '%d%m%Y')
    except ValueError:
        raise ValueError("Date format must be DDMMYYYY")
    
    # Construct URL
    url = f"https://www.nseindia.com/api/bhavCopy?symbol=EQ&date={date}"
    
    # Set up session with cookies
    session = requests.Session()
    
    # First request to get cookies
    response = session.get(url)
    if not response.ok:
        raise Exception(f"Failed to get data for {date}")
    
    # Second request with cookies
    response = session.get(url, params={'symbol': 'EQ', 'date': date})
    if not response.ok:
        raise Exception(f"Failed to get bhavcopy data for {date}")
    
    # Get the CSV content
    csv_content = response.text
    
    # Parse CSV
    df = pd.read_csv(pd.io.common.StringIO(csv_content))
    
    # Clean and process columns
    df = df[['SC_CODE', 'SC_NAME', 'OPEN', 'HIGH', 'LOW', 'CLOSE', 'LAST', 'VOLUME', 'TOTALTRADEVALUE']]
    
    # Convert to numeric types
    df['OPEN'] = pd.to_numeric(df['OPEN'])
    df['HIGH'] = pd.to_numeric(df['HIGH'])
    df['LOW'] = pd.to_numeric(df['LOW'])
    df['CLOSE'] = pd.to_numeric(df['CLOSE'])
    df['VOLUME'] = pd.to_numeric(df['VOLUME'])
    
    # Add adjusted columns (NSE data doesn't have adjusted values, so we'll calculate from close)
    df['adjOpen'] = df['OPEN']
    df['adjHigh'] = df['HIGH']
    df['adjLow'] = df['LOW']
    df['adjClose'] = df['CLOSE']
    
    return df

def fetch_yahoo_eod(symbol: str, date: str):
    """Fetch daily OHLCV from Yahoo Finance using yfinance."""
    try:
        # Download data for the specific date
        data = yf.download(symbol, start=date, end=date)
        if data.empty:
            raise ValueError(f"No data found for {symbol} on {date}")
        
        # Ensure we have exactly one day of data
        data = data.iloc[0:1]
        
        # Rename columns to match bhavcopy format
        data = data.rename(columns={
            'Open': 'open',
            'High': 'high',
            'Low': 'low',
            'Close': 'close',
            'Volume': 'volume'
        })
        
        # Add adjusted columns (yfinance handles splits/dividends automatically)
        data['adjOpen'] = data['Open']
        data['adjHigh'] = data['High']
        data['adjLow'] = data['Low']
        data['adjClose'] = data['Close']
        
        return data
    except Exception as e:
        raise ValueError(f"Failed to fetch Yahoo data for {symbol} on {date}: {str(e)}")

def merge_eod(date: str, symbols: list):
    """Merge EOD data from NSE and Yahoo, validate and prefer NSE data."""
    # Fetch NSE data first
    nse_df = fetch_bhavcopy(date)
    
    # Initialize result DataFrame
    merged_df = pd.DataFrame()
    
    for symbol in symbols:
        try:
            # Fetch Yahoo data
            yahoo_df = fetch_yahoo_eod(symbol, date)
            
            # Merge on symbol and date
            merged = pd.merge(
                nse_df,
                yahoo_df,
                left_on=['SC_CODE', 'date'],
                right_on=['symbol', 'Date'],
                how='outer',
                suffixes=('_nse', '_yahoo')
            )
            
            # Validate close price difference
            if not merged.empty:
                nse_close = merged['CLOSE_nse'].iloc[0]
                yahoo_close = merged['close_yahoo'].iloc[0]
                
                if abs((nse_close - yahoo_close) / nse_close) > 0.005:
                    print(f"Warning: Price discrepancy for {symbol} on {date}: NSE={nse_close}, Yahoo={yahoo_close}")
                
                # Prefer NSE data where available
                merged['open'] = merged['open_nse'].combine_first(merged['open_yahoo'])
                merged['high'] = merged['high_nse'].combine_first(merged['high_yahoo'])
                merged['low'] = merged['low_nse'].combine_first(merged['low_yahoo'])
                merged['close'] = merged['close_nse'].combine_first(merged['close_yahoo'])
                merged['volume'] = merged['volume_nse'].combine_first(merged['volume_yahoo'])
                merged['adjOpen'] = merged['adjOpen_nse'].combine_first(merged['adjOpen_yahoo'])
                merged['adjHigh'] = merged['adjHigh_nse'].combine_first(merged['adjHigh_yahoo'])
                merged['adjLow'] = merged['adjLow_nse'].combine_first(merged['adjLow_yahoo'])
                merged['adjClose'] = merged['adjClose_naw'] combine_first(merged['adjClose_yahoo'])
                
                # Keep only the NSE symbol and date columns
                merged = merged[['SC_CODE', 'date', 'open', 'high', 'low', 'close', 'volume', 
                                'adjOpen', 'adjHigh', 'adjLow', 'adjClose']]
                
                merged_df = pd.concat([merged_df, merged], ignore_index=True)
                
        except Exception as e:
            print(f"Error processing {symbol} on {date}: {str(e)}")
            continue
    
    return merged_df

def validate_date(date: str):
    """Check if date is a trading day using NSE calendar."""
    try:
        datetime.strptime(date, '%d%m%Y')
    except ValueError:
        return False
    
    # Convert to datetime object
    dt = datetime.strptime(date, '%d%m%Y')
    
    # Check if it's in holidays list
    if dt.strftime('%Y-%m-%d') in holidays:
        return False
    
    return True

def run():
    """Orchestrates the EOD pipeline."""
    # Get today's date
    today = datetime.now().strftime('%d%m%Y')
    
    # Validate trading day
    if not validate_date(today):
        print(f"Warning: {today} is a holiday, skipping EOD pipeline")
        return
    
    # Get all 500 symbols from NSE
    symbols = ['^BSESN', 'NIFTY50']  # Placeholder, should be actual list
    
    # Fetch and merge data
    merged_data = merge_eod(today, symbols)
    
    # Save to parquet
    merged_data.to_parquet('history.parquet', index=False)
    
    # Upload to Kaggle dataset
    kaggle.upload(f"EOD data - {today}")</think>
<think>I need to check the current content of the eod.py file first to see what needs to be added or modified.
<tool_call>read
<arg_key>path</arg_key>
<arg_value>pipeline/eod.py