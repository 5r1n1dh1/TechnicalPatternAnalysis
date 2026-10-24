"""Shared bar loader for endpoints (no module-level yfinance/pandas import)."""

def load_bars(symbol: str, interval: str = "1d", range_str: str = "1Y"):
    import pandas as pd
    try:
        import yfinance as yf
        ticker = yf.Ticker(symbol + ".NS" if not symbol.endswith((".NS", ".BO")) else symbol)
        period_map = {"1D":"1d","5D":"5d","1M":"1mo","3M":"3mo","6M":"6mo","1Y":"1y","2Y":"2y","5Y":"5y","10Y":"max"}
        iv_map = {"5m":"5m","15m":"15m","30m":"30m","1h":"60m","1d":"1d","1w":"1wk","1M":"1mo"}
        df = ticker.history(period=period_map.get(range_str,"1y"), interval=iv_map.get(interval,"1d"))
        if df is None or df.empty:
            return None
        # yfinance >=0.2.51 returns a MultiIndex (Price, Ticker); flatten it or
        # the column renames below silently never match.
        if getattr(df.columns, "nlevels", 1) > 1:
            df.columns = df.columns.get_level_values(0)
        df = df.reset_index()
        if "Date" in df.columns:
            df = df.rename(columns={"Date":"t"})
        elif "Datetime" in df.columns:
            df = df.rename(columns={"Datetime":"t"})
        elif "time" in df.columns:
            df = df.rename(columns={"time":"t"})
        df = df.rename(columns={"Open":"o","High":"h","Low":"l","Close":"c","Volume":"v"})
        df = df[["t","o","h","l","c","v"]].copy()
        df["t"] = pd.to_datetime(df["t"])
        return df
    except Exception:
        return None
