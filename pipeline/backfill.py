# Backfill script to load 10 years of adjusted daily bars from Yahoo

import os
import pandas as pd
import yfinance as yf
from datetime import datetime
import json

# Define the time range (10 years ago to today)
start_date = (datetime.now() - pd.Timedelta(days=3650)).strftime('%Y-%m-%d')
end_date = datetime.now().strftime('%Y-%m-%d')

# Get all 500 symbols from NSE (placeholder - should be actual list)
symbols = ['^BSESN', 'NIFTY50']  # Placeholder

def backfill_data():
    """Load 10 years of adjusted daily bars from Yahoo."""
    all_data = []
    
    for symbol in symbols:
        try:
            # Download data with adjusted=True to handle splits/dividends
            data = yf.download(symbol, start=start_date, end=end_date, adjusted=True)
            
            if not data.empty:
                # Add symbol column
                data['symbol'] = symbol
                all_data.append(data)
                
        except Exception as e:
            print(f"Error downloading data for {symbol}: {str(e)}")
            continue
    
    if all_data:
        # Concatenate all data
        combined_data = pd.concat(all_data, ignore_index=True)
        
        # Save to parquet
        combined_data.to_parquet('history.parquet', index=False)
        
        # Upload to Kaggle
        kaggle.upload(f"Backfilled data - 10 years ({start_date} to {end_date})")
    else:
        print("No data downloaded")

if __name__ == "__main__":
    backfill_data()