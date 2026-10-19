# Backfill script to load 10 years of adjusted daily bars from Yahoo

import os
import pandas as pd
import yfinance as yf
from datetime import datetime


start_date = (datetime.now() - pd.Timedelta(days=3650)).strftime("%Y-%m-%d")
end_date = datetime.now().strftime("%Y-%m-%d")

# Placeholder symbols
symbols = ["^BSESN", "NIFTY50"]


def backfill_data():
    all_data = []
    for symbol in symbols:
        try:
            data = yf.download(symbol, start=start_date, end=end_date, adjusted=True, progress=False)
            if not data.empty:
                data = data.reset_index()
                data["symbol"] = symbol
                all_data.append(data)
        except Exception as e:
            print(f"Error downloading data for {symbol}: {str(e)}")
            continue

    if all_data:
        combined_data = pd.concat(all_data, ignore_index=True)
        combined_data.to_parquet("history.parquet", index=False)
        print(f"Backfilled data saved to history.parquet")
    else:
        print("No data downloaded")


if __name__ == "__main__":
    backfill_data()
