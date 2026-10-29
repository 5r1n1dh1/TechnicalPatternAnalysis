"""Intraday bars from keyless providers.

Yahoo's chart endpoint is used directly (no cookie/crumb handshake, which is
what makes yfinance fail on Vercel). Groww's delayed charting endpoint is the
second provider and covers roughly the last 30 days.

Both providers return UTC timestamps; callers convert to IST. Columns are
normalised to ``t, o, h, l, c, v``.
"""

from __future__ import annotations

import time
from typing import Optional, Tuple

# provider request timeout (seconds); Vercel functions are short-lived
_TIMEOUT = 3.5
_CACHE_TTL = 60.0
_NEGATIVE_TTL = 30.0
_MIN_BARS = 10

# calendar days requested from the provider for each API range
_RANGE_DAYS = {
    "1D": 5,
    "5D": 10,
    "1M": 35,
    "3M": 100,
    "6M": 200,
    "1Y": 370,
}

# Yahoo enums
_YAHOO_RANGE = {
    "1D": "1d",
    "5D": "5d",
    "1M": "1mo",
    "3M": "3mo",
    "6M": "6mo",
    "1Y": "1y",
}
_YAHOO_INTERVAL = {"5m": "5m", "15m": "15m", "30m": "30m", "1h": "60m"}
_GROWW_MINUTES = {"5m": 5, "15m": 15, "30m": 30, "1h": 60}

_CACHE: dict = {}

_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)


def _bare(symbol: str) -> str:
    s = str(symbol).strip().upper()
    for suffix in (".NS", ".BO"):
        if s.endswith(suffix):
            return s[: -len(suffix)]
    return s


def _yahoo_symbol(symbol: str) -> str:
    s = str(symbol).strip().upper()
    return s if s.endswith((".NS", ".BO")) else s + ".NS"


def _last_session(df):
    """Keep only the most recent IST trading day (used for the 1D range)."""
    import pandas as pd

    if df is None or df.empty:
        return df
    ist = pd.to_datetime(df["t"], utc=True).dt.tz_convert("Asia/Kolkata")
    last_day = ist.dt.date.max()
    return df[ist.dt.date == last_day].reset_index(drop=True)


def _from_yahoo(symbol: str, interval: str, range_str: str):
    import httpx
    import pandas as pd

    y_interval = _YAHOO_INTERVAL[interval]
    y_range = _YAHOO_RANGE[range_str]

    def request(rng: str):
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{_yahoo_symbol(symbol)}"
        params = {
            "interval": y_interval,
            "range": rng,
            "includePrePost": "false",
            "events": "div,splits",
        }
        response = httpx.get(
            url,
            params=params,
            headers={"User-Agent": _UA, "Accept": "application/json"},
            timeout=_TIMEOUT,
            follow_redirects=True,
        )
        response.raise_for_status()
        return response.json()

    payload = request(y_range)
    result = (payload.get("chart") or {}).get("result") or []
    if not result:
        error = (payload.get("chart") or {}).get("error")
        if error:
            raise ValueError(f"yahoo: {error}")
        result = []

    # Pre-market / weekend: 1d may be empty, fall back to the last 5 sessions.
    if not result and range_str == "1D":
        payload = request("5d")
        result = (payload.get("chart") or {}).get("result") or []

    if not result:
        return None

    node = result[0]
    timestamps = node.get("timestamp") or []
    quotes = ((node.get("indicators") or {}).get("quote") or [{}])[0]
    opens = quotes.get("open") or []
    highs = quotes.get("high") or []
    lows = quotes.get("low") or []
    closes = quotes.get("close") or []
    volumes = quotes.get("volume") or []

    rows = []
    for i, ts in enumerate(timestamps):
        o = opens[i] if i < len(opens) else None
        h = highs[i] if i < len(highs) else None
        low = lows[i] if i < len(lows) else None
        c = closes[i] if i < len(closes) else None
        if None in (o, h, low, c):
            continue
        v = volumes[i] if i < len(volumes) and volumes[i] is not None else 0
        rows.append((ts, float(o), float(h), float(low), float(c), float(v)))

    if not rows:
        return None

    df = pd.DataFrame(rows, columns=["t", "o", "h", "l", "c", "v"])
    df["t"] = pd.to_datetime(df["t"], unit="s", utc=True)
    df = df.sort_values("t").drop_duplicates("t").reset_index(drop=True)
    if range_str == "1D":
        df = _last_session(df)
    return df


def _from_groww(symbol: str, interval: str, range_str: str):
    import httpx
    import pandas as pd

    minutes = _GROWW_MINUTES[interval]
    days = _RANGE_DAYS[range_str]
    end_ms = int(time.time() * 1000)
    start_ms = end_ms - days * 86_400_000
    url = (
        "https://groww.in/v1/api/charting_service/v2/chart/delayed/"
        f"exchange/NSE/segment/CASH/{_bare(symbol)}"
    )
    response = httpx.get(
        url,
        params={
            "intervalInMinutes": minutes,
            "startTimeInMillis": start_ms,
            "endTimeInMillis": end_ms,
        },
        headers={"User-Agent": _UA, "Accept": "application/json"},
        timeout=_TIMEOUT,
        follow_redirects=True,
    )
    response.raise_for_status()
    candles = (response.json() or {}).get("candles") or []
    if not candles:
        return None

    df = pd.DataFrame(candles, columns=["t", "o", "h", "l", "c", "v"])
    df["t"] = pd.to_datetime(df["t"], unit="s", utc=True)
    for col in ("o", "h", "l", "c", "v"):
        df[col] = pd.to_numeric(df[col], errors="coerce")
    df = df.dropna(subset=["o", "h", "l", "c"]).fillna({"v": 0})
    df = df.sort_values("t").drop_duplicates("t").reset_index(drop=True)

    # Groww reports cumulative volume for the session; convert to per-bar.
    ist = df["t"].dt.tz_convert("Asia/Kolkata")
    previous = 0.0
    previous_day = None
    volumes = []
    for day, cumulative in zip(ist.dt.date, df["v"]):
        if day != previous_day:
            previous = 0.0
            previous_day = day
        value = float(cumulative) if cumulative is not None else 0.0
        if value <= 0:
            # Provider sometimes omits a bar's cumulative value; keep the last
            # known total instead of treating the gap as a day reset.
            volumes.append(0.0)
            continue
        step = value - previous
        previous = value
        volumes.append(max(step, 0.0))
    df["v"] = volumes

    if range_str == "1D":
        df = _last_session(df)
    return df.reset_index(drop=True)


def fetch_intraday(symbol: str, interval: str, range_str: str) -> Tuple[Optional[object], Optional[str]]:
    """Return ``(DataFrame, source)`` for an intraday pair, or ``(None, None)``.

    Results are cached for 60 s so the three parallel chart calls (candles,
    trend, patterns) do not hit the providers three times.
    """
    if interval not in _YAHOO_INTERVAL:
        return None, None
    key = (_bare(symbol), interval, str(range_str).upper())
    now = time.monotonic()
    cached = _CACHE.get(key)
    if cached is not None:
        stored_at, frame, source = cached
        ttl = _CACHE_TTL if frame is not None else _NEGATIVE_TTL
        if now - stored_at < ttl:
            return (frame.copy() if frame is not None else None), source

    frame, source = None, None
    for provider, name in ((_from_yahoo, "yahoo"), (_from_groww, "groww")):
        try:
            candidate = provider(symbol, interval, range_str)
            if candidate is not None and len(candidate) >= _MIN_BARS:
                frame, source = candidate, name
                break
        except Exception:
            continue

    _CACHE[key] = (now, frame, source)
    return (frame.copy() if frame is not None else None), source


def clear_cache() -> None:
    _CACHE.clear()
