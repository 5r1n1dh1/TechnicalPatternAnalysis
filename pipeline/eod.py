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


def load_universe():
    """Load NSE tickers (Nifty-500) + ^NSEI benchmark from server/data/symbols.json.
    Each entry: {"symbol": "RELIANCE", "name": "...", "sector": "..."}.
    Returns a list of uppercase symbol strings suitable for yfinance."""
    symbols_path = os.path.join(
        os.path.dirname(__file__), "..", "server", "data", "symbols.json"
    )
    if not os.path.exists(symbols_path):
        print("Warning: symbols.json not found; falling back to minimal universe")
        return ["^NSEI"]
    try:
        with open(symbols_path, "r") as f:
            raw = json.load(f)
        tickers = [str(s["symbol"]).strip().upper() for s in raw]
        # Dedupe (preserving order) and append NSEI benchmark if absent
        seen = set()
        tickers = [t for t in tickers if not (t in seen or seen.add(t))]
        if "^NSEI" not in tickers:
            tickers.append("^NSEI")
        return tickers
    except Exception as e:
        print(f"Warning: could not parse symbols.json ({e}); using minimal universe")
        return ["^NSEI"]


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
    """Fetch delivery % from NSE sec_bhavdata_full CSV for the trading day.
    Returns a DataFrame[columns: SC_CODE, delivery_pct] with delivery % (0-100).
    Returns an empty DataFrame on any failure so callers can treat it as "no delivery data".
    """
    try:
        datetime.strptime(date, "%d%m%Y")
    except ValueError:
        return pd.DataFrame(columns=["SC_CODE", "delivery_pct"])

    # NSE sec_bhavdata_full: columns include DELIV_PER (delivery %),
    # and the symbol is in the SYMBOL column (not SC_CODE).
    # Try two known NSE endpoints for the daily full bhavcopy.
    ymd = datetime.strptime(date, "%d%m%Y").strftime("%Y-%m-%d")
    endpoints = [
        f"https://www.nseindia.com/api/sec-bhavdata-full?symbol=ALL&date={date}",
        f"https://www.nseindia.com/api/sec-bhavdata-full?date={date}",
    ]

    session = requests.Session()
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
        "Accept": "application/json,text/csv,application/vnd.ms-excel",
    }
    # Cookie handshake — required by NSE endpoints
    try:
        session.get("https://www.nseindia.com/", headers=headers, timeout=30)
    except Exception:
        pass

    response = None
    for url in endpoints:
        try:
            response = session.get(url, headers=headers, timeout=30)
            if response is not None and getattr(response, "ok", False):
                break
        except Exception:
            response = None

    if response is None or not getattr(response, "ok", False):
        print(f"Warning: NSE delivery data unavailable for {date}; deliveryPct will be null")
        return pd.DataFrame(columns=["SC_CODE", "delivery_pct"])

    try:
        # NSE returns JSON; fall back to CSV parsing
        try:
            records = response.json()
        except Exception:
            df = pd.read_csv(pd.io.common.StringIO(response.text))
        else:
            if isinstance(records, dict) and "data" in records:
                records = records["data"]
            if isinstance(records, list) and records:
                df = pd.DataFrame(records)
            else:
                df = pd.DataFrame()
    except Exception:
        return pd.DataFrame(columns=["SC_CODE", "delivery_pct"])

    if df.empty:
        return pd.DataFrame(columns=["SC_CODE", "delivery_pct"])

    # Normalize: SEC code column may be "SYMBOL" or "SC_CODE"; delivery column "DELIV_PER" or "delivery"
    out = pd.DataFrame()
    sym_col = None
    deliv_col = None
    for c in df.columns:
        cu = str(c).upper().strip()
        if cu in ("SYMBOL", "SC_CODE") and sym_col is None:
            sym_col = c
        if cu in ("DELIV_PER", "DELIV_PERCENT", "DELIVERY_PER", "DELIVERY") and deliv_col is None:
            deliv_col = c
    if sym_col is None or deliv_col is None:
        # Try to infer
        for c in df.columns:
            if str(c).upper().strip() == "SYMBOL":
                sym_col = c
        for c in df.columns:
            cu = str(c).upper().strip()
            if "DELIV" in cu or cu == "DELIVERY_PERCENT":
                deliv_col = c

    if sym_col and deliv_col:
        out = pd.DataFrame({
            "SC_CODE": df[sym_col].astype(str).str.strip().str.upper(),
            "delivery_pct": pd.to_numeric(df[deliv_col], errors="coerce"),
            "date": ymd,
        })
        # Drop rows with NaN delivery_pct
        out = out.dropna(subset=["delivery_pct"])
        # Deduplicate by SC_CODE (keep first / latest non-null)
        out = out.drop_duplicates(subset="SC_CODE", keep="first")
    return out


def run():
    today = datetime.now().strftime("%d%m%Y")
    if not validate_date(today):
        print(f"Warning: {today} is a holiday, skipping EOD pipeline")
        return pd.DataFrame()
    # Load real NSE universe (Nifty-500 + ^NSEI benchmark) — fixes partial universe capture (v1.0.3 T-29)
    symbols = load_universe()
    if "^NSEI" not in symbols:
        symbols.append("^NSEI")
    merged_data = merge_eod(today, symbols)
    if merged_data is None or merged_data.empty:
        print("No new EOD data available; nothing to commit")
        return pd.DataFrame()
    
    # Coverage guard: if fewer than 80% of the universe was captured, this is a
    # data integrity failure — exit non-zero so the workflow step fails loudly
    # instead of silently shipping a partial universe (v1.0.3 T-29).
    captured = merged_data["SC_CODE"].nunique()
    expected = len(symbols)
    coverage = captured / expected if expected else 0.0
    print(f"Coverage: {captured}/{expected} symbols ({coverage:.1%})")
    if coverage < 0.8:
        raise RuntimeError(
            f"EOD coverage too low: {captured}/{expected} symbols captured "
            f"({coverage:.1%}); aborting (minimum 80% required)"
        )
    
    # Fetch delivery % from NSE sec_bhavdata_full (best-effort; NaN on failure)
    delivery_df = fetch_sec_delivery_pct(today)
    if not delivery_df.empty:
        merged_data = merged_data.merge(
            delivery_df[["SC_CODE", "delivery_pct"]],
            on="SC_CODE", how="left"
        )
        merged_data["deliveryCaptured"] = True
    else:
        merged_data["deliveryPct"] = np.nan
        merged_data["deliveryCaptured"] = False
    
    try:
        # Append + versioned if history exists
        out_path = "history.parquet"
        if os.path.exists(out_path):
            try:
                hist = pd.read_parquet(out_path)
                merged_data = pd.concat([hist, merged_data], ignore_index=True)
                subset = [c for c in ("SC_CODE", "date") if c in merged_data.columns]
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
        raise
    
    print(f"Date range: {merged_data['date'].min()} to {merged_data['date'].max()}")
    return merged_data


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "validate_date":
        d = sys.argv[2] if len(sys.argv) > 2 else datetime.now().strftime("%d%m%Y")
        print(validate_date(d))
    else:
        run()
