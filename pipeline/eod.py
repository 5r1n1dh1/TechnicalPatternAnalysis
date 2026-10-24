# EOD data processing
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
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
        # NSE source unavailable/blocked: degrade gracefully instead of aborting
        # the whole pipeline. The Yahoo leg in merge_eod still runs.
        print(f"Warning: NSE bhavcopy unavailable for {date}; continuing without NSE data")
        return pd.DataFrame()

    try:
        csv_content = response.text
        df = pd.read_csv(pd.io.common.StringIO(csv_content))
    except Exception:
        # Try parsing as json if csv fails
        df = pd.DataFrame()

    if df.empty:
        print(f"Warning: No NSE data returned for {date}; continuing without NSE data")
        return pd.DataFrame()

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
        # yf.download returns a MultiIndex (Price, Ticker) for a single symbol;
        # flatten it or the renames below produce Series-valued columns that fail
        # the Arrow write with "did not recognize Python value type".
        if getattr(data.columns, "nlevels", 1) > 1:
            data.columns = data.columns.get_level_values(0)
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


def _normalize_df(df):
    if df is None or df.empty:
        return df
    rename_map = {}
    for c in df.columns:
        lc = str(c).lower()
        if lc in ("open", "high", "low", "close", "volume") and str(c) != lc:
            rename_map[c] = lc
    if rename_map:
        df = df.rename(columns=rename_map)
    return df


def merge_eod(date: str, symbols: list):
    nse_df = fetch_bhavcopy(date)
    ymd = datetime.strptime(date, "%d%m%Y").strftime("%Y-%m-%d")
    nse_by_symbol = {}
    if nse_df is not None and not nse_df.empty and "SC_CODE" in nse_df.columns:
        for _, row in nse_df.iterrows():
            key = str(row.get("SC_CODE", "")).strip().upper()
            if key:
                nse_by_symbol[key] = row

    rows = []
    for symbol in symbols:
        try:
            yahoo_df = _normalize_df(fetch_yahoo_eod(symbol, date))
            if yahoo_df is None or yahoo_df.empty:
                continue
            yrow = yahoo_df.iloc[-1]
            sym_key = str(symbol).strip().upper()
            nse_row = nse_by_symbol.get(sym_key)

            open_ = yrow.get("open") if "open" in yrow.index else np.nan
            high_ = yrow.get("high") if "high" in yrow.index else np.nan
            low_ = yrow.get("low") if "low" in yrow.index else np.nan
            close_ = yrow.get("close") if "close" in yrow.index else np.nan
            vol = yrow.get("volume") if "volume" in yrow.index else np.nan

            if nse_row is not None:
                nse_close_raw = nse_row.get("CLOSE") if hasattr(nse_row, "get") else np.nan
                y_close_raw = close_
                nse_close = pd.to_numeric(nse_close_raw, errors="coerce")
                y_close = pd.to_numeric(y_close_raw, errors="coerce")
                if pd.notna(nse_close) and pd.notna(y_close) and float(nse_close) > 0:
                    diff = abs(float(nse_close) - float(y_close)) / float(nse_close)
                    if diff > 0.005:
                        print(f"Warning: Price discrepancy for {symbol} on {ymd}: diff={diff:.4%}")
                        continue
                open_ = nse_row.get("OPEN") if hasattr(nse_row, "get") and pd.notna(pd.to_numeric(nse_row.get("OPEN"), errors="coerce")) else open_
                high_ = nse_row.get("HIGH") if hasattr(nse_row, "get") and pd.notna(pd.to_numeric(nse_row.get("HIGH"), errors="coerce")) else high_
                low_ = nse_row.get("LOW") if hasattr(nse_row, "get") and pd.notna(pd.to_numeric(nse_row.get("LOW"), errors="coerce")) else low_
                close_ = nse_row.get("CLOSE") if hasattr(nse_row, "get") else close_
                vol = nse_row.get("VOLUME") if hasattr(nse_row, "get") else vol

            open_ = pd.to_numeric(open_, errors="coerce")
            high_ = pd.to_numeric(high_, errors="coerce")
            low_ = pd.to_numeric(low_, errors="coerce")
            close_ = pd.to_numeric(close_, errors="coerce")
            vol = pd.to_numeric(vol, errors="coerce")

            rows.append({
                "SC_CODE": sym_key,
                "date": ymd,
                "open": open_,
                "high": high_,
                "low": low_,
                "close": close_,
                "volume": vol,
                "adjOpen": open_,
                "adjHigh": high_,
                "adjLow": low_,
                "adjClose": close_,
            })
        except Exception as e:
            print(f"Error processing {symbol} on {date}: {str(e)}")
            continue

    return pd.DataFrame(rows)


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
        return pd.DataFrame()
    # ^NSEI is the Yahoo ticker for NIFTY 50; "NIFTY50" is not a valid symbol.
    symbols = ["^BSESN", "^NSEI"]
    merged_data = merge_eod(today, symbols)
    if merged_data is None or merged_data.empty:
        print("No new EOD data available; nothing to commit")
        return pd.DataFrame()
    try:
        # Append + versioned if history exists
        out_path = "history.parquet"
        if os.path.exists(out_path):
            try:
                hist = pd.read_parquet(out_path)
                merged_data = pd.concat([hist, merged_data], ignore_index=True)
                subset = [c for c in ("SC_CODE", "symbol", "date") if c in merged_data.columns]
                if subset:
                    merged_data = merged_data.drop_duplicates(subset=subset, keep="last")
            except Exception as e:
                print(f"Warning: could not merge existing history: {e}")
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
