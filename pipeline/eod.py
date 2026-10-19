# EOD data processing

import os
import json
from datetime import datetime, timedelta

import pandas as pd
import numpy as np
import requests
import yfinance as yf
from dateutil.relativedelta import relativedelta


# Load NSE holidays from bundled calendar JSON
NSE_CALENDAR_PATH = os.path.join(os.path.dirname(__file__), "..", "server", "core", "nse_holidays.json")
NSE_CALENDAR_PATH = os.path.abspath(NSE_CALENDAR_PATH)
if not os.path.exists(NSE_CALENDAR_PATH):
    # Fallback to alternative location
    NSE_CALENDAR_PATH = os.path.join(os.path.dirname(__file__), "nse_holidays.json")
    NSE_CALENDAR_PATH = os.path.abspath(NSE_CALENDAR_PATH)

holidays = set()
try:
    with open(NSE_CALENDAR_PATH, "r") as f:
        cal = json.load(f)
        # calendar can be list of dates or dict; support both
        if isinstance(cal, dict):
            hlist = cal.get("holidays", cal.get("dates", []))
        else:
            hlist = cal
        for d in hlist:
            try:
                # Try parsing YYYY-MM-DD
                dt = datetime.strptime(str(d), "%Y-%m-%d")
                holidays.add(dt.strftime("%Y-%m-%d"))
            except Exception:
                continue
except Exception:
    holidays = set()


def fetch_bhavcopy(date: str):
    """Fetch NSE bhavcopy CSV via requests, handles cookie handshake."""
    try:
        datetime.strptime(date, "%d%m%Y")
    except ValueError:
        raise ValueError("Date format must be DDMMYYYY")

    url = "https://www.nseindia.com/api/bhavCopy"
    params = {"symbol": "EQ", "date": date}

    session = requests.Session()
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
        "Accept": "application/json,text/csv,application/vnd.ms-excel",
    }
    # Cookie handshake - first request to get cookies
    try:
        session.get("https://www.nseindia.com/", headers=headers, timeout=30)
        response = session.get(url, params=params, headers=headers, timeout=30)
    except Exception:
        response = None

    if response is None or not getattr(response, "ok", False):
        raise Exception(f"Failed to get data for {date}")

    try:
        csv_content = response.text
        df = pd.read_csv(pd.io.common.StringIO(csv_content))
    except Exception:
        # Try parsing as json if csv fails
        df = pd.DataFrame()

    if df.empty:
        raise Exception(f"No data returned for {date}")

    # Keep expected columns if they exist
    keep_cols = ["SC_CODE", "SC_NAME", "OPEN", "HIGH", "LOW", "CLOSE", "LAST", "VOLUME", "TOTALTRADEVALUE"]
    cols = [c for c in keep_cols if c in df.columns]
    if not cols:
        # try lowercase
        df.columns = [c.upper() for c in df.columns]
        cols = [c for c in keep_cols if c in df.columns]
    df = df[cols].copy() if cols else df.copy()

    for c in ["OPEN", "HIGH", "LOW", "CLOSE", "VOLUME"]:
        if c in df.columns:
            df[c] = pd.to_numeric(df[c], errors="coerce")

    df["adjOpen"] = df["OPEN"]
    df["adjHigh"] = df["HIGH"]
    df["adjLow"] = df["LOW"]
    df["adjClose"] = df["CLOSE"]

    df["date"] = datetime.strptime(date, "%d%m%Y").strftime("%Y-%m-%d")

    return df


def fetch_yahoo_eod(symbol: str, date: str):
    try:
        dt = datetime.strptime(date, "%d%m%Y")
        start = (dt - timedelta(days=1)).strftime("%Y-%m-%d")
        end = (dt + timedelta(days=1)).strftime("%Y-%m-%d")
        data = yf.download(symbol, start=start, end=end, progress=False)
        if data.empty:
            raise ValueError(f"No data found for {symbol} on {date}")
        data = data.iloc[-1:]
        data = data.rename(
            columns={
                "Open": "open",
                "High": "high",
                "Low": "low",
                "Close": "close",
                "Volume": "volume",
            }
        )
        data["adjOpen"] = data["open"]
        data["adjHigh"] = data["high"]
        data["adjLow"] = data["low"]
        data["adjClose"] = data["close"]
        data["symbol"] = symbol
        data["Date"] = dt.strftime("%Y-%m-%d")
        return data
    except Exception as e:
        raise ValueError(f"Failed to fetch Yahoo data for {symbol} on {date}: {str(e)}")


def merge_eod(date: str, symbols: list):
    nse_df = fetch_bhavcopy(date)
    merged_df = pd.DataFrame()
    ymd = datetime.strptime(date, "%d%m%Y").strftime("%Y-%m-%d")

    for symbol in symbols:
        try:
            yahoo_df = fetch_yahoo_eod(symbol, date)
            if nse_df.empty:
                rec = yahoo_df.copy()
                rec["SC_CODE"] = symbol
                rec["date"] = ymd
                merged_df = pd.concat([merged_df, rec], ignore_index=True)
                continue

            # Try to merge by symbol code/name - simplified
            merged = yahoo_df.copy()
            merged["SC_CODE"] = symbol
            merged["date"] = ymd
            merged["open_nse"] = nse_df["OPEN"].iloc[0] if "OPEN" in nse_df else np.nan
            merged["close_nse"] = nse_df["CLOSE"].iloc[0] if "CLOSE" in nse_df else np.nan
            nse_close = merged["close_nse"].iloc[0] if not merged.empty else np.nan
            yahoo_close = merged["close"].iloc[0] if not merged.empty else np.nan

            if pd.notna(nse_close) and pd.notna(yahoo_close) and nse_close > 0:
                diff = abs(nse_close - yahoo_close) / nse_close
                if diff > 0.005:
                    raise ValueError(f"Price discrepancy breach > 0.5% for {symbol} on {date}: diff={diff:.4%}")

            # Prefer NSE where available
            merged["open"] = merged["open_nse"].fillna(merged["open"])
            merged["high"] = merged.get("high_nse", merged["high"])
            merged["low"] = merged.get("low_nse", merged["low"])
            merged["close"] = merged["close_nse"].fillna(merged["close"])
            merged["volume"] = merged["volume_nse"].fillna(merged["volume"]) if "volume_nse" in merged else merged["volume"]
            merged["adjOpen"] = merged["open"]
            merged["adjHigh"] = merged["high"]
            merged["adjLow"] = merged["low"]
            merged["adjClose"] = merged["close"]

            keep = ["SC_CODE", "date", "open", "high", "low", "close", "volume", "adjOpen", "adjHigh", "adjLow", "adjClose"]
            merged = merged[[c for c in keep if c in merged.columns]]
            merged_df = pd.concat([merged_df, merged], ignore_index=True)
        except Exception as e:
            print(f"Error processing {symbol} on {date}: {str(e)}")
            continue

    return merged_df


def validate_date(date: str):
    try:
        datetime.strptime(date, "%d%m%Y")
    except ValueError:
        return False
    dt = datetime.strptime(date, "%d%m%Y")
    if dt.strftime("%Y-%m-%d") in holidays:
        return False
    return True


def fetch_sec_delivery_pct(date: str):
    """Fetch delivery % from sec_bhavdata_full - stub implementation."""
    try:
        ymd = datetime.strptime(date, "%d%m%Y").strftime("%Y-%m-%d")
        # In production, this would fetch from NSE/SEC bhavdata full
        # Return empty df if not available
        return pd.DataFrame(columns=["symbol", "delivery_pct", "date"])
    except Exception:
        return pd.DataFrame()


def run():
    today = datetime.now().strftime("%d%m%Y")
    if not validate_date(today):
        print(f"Warning: {today} is a holiday, skipping EOD pipeline")
        return
    symbols = ["^BSESN", "NIFTY50"]
    merged_data = merge_eod(today, symbols)
    try:
        # Append + versioned if history exists
        out_path = "history.parquet"
        if os.path.exists(out_path):
            try:
                hist = pd.read_parquet(out_path)
                merged_data = pd.concat([hist, merged_data], ignore_index=True)
                merged_data = merged_data.drop_duplicates(subset=["symbol", "date"], keep="last")
            except Exception:
                pass
        merged_data.to_parquet(out_path, index=False)
        print(f"Saved {len(merged_data)} rows to {out_path}")
        # Create versioned copy
        ver_path = f"history_{datetime.now().strftime('%Y%m%d_%H%M%S')}.parquet"
        try:
            merged_data.to_parquet(ver_path, index=False)
        except Exception:
            pass
    except Exception as e:
        print(f"Error saving parquet: {e}")
    # Try to fetch delivery data
    try:
        fetch_sec_delivery_pct(today)
    except Exception:
        pass
    # Kaggle upload stub - would require kaggle API setup
    try:
        os.environ.get("KAGGLE_USERNAME")  # touch env
    except Exception:
        pass
    return merged_data


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "validate_date":
        d = sys.argv[2] if len(sys.argv) > 2 else datetime.now().strftime("%d%m%Y")
        print(validate_date(d))
    else:
        run()
