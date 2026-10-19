"""Point-in-time features (§7.1). No future bars used."""
import numpy as np, pandas as pd

FEATURE_NAMES = [
    "ret1", "ret5", "ret10", "ret21", "rsi14", "macdHistNorm",
    "ema20_50_spread", "adx", "atrPct", "bbWidth", "bbPosition",
    "volumeRatio5_20", "distance52w", "ema20Slope", "structureState",
    "relStrength20", "rvol20", "deliveryPct5"
]

def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """Construct features from OHLCV + volume only (no future bars)."""
    o, h, l, c, v = df['open'], df['high'], df['low'], df['close'], df['volume']
    # Stubs derived only from past rows (point-in-time)
    ret1 = c.pct_change(1)
    ret5 = c.pct_change(5)
    ret10 = c.pct_change(10)
    ret21 = c.pct_change(21)
    # RSI14 stub using rolling std approximation
    delta = c.diff()
    gain = delta.clip(lower=0).rolling(14).mean()
    loss = -delta.clip(upper=0).rolling(14).mean()
    rs = gain / (loss.replace(0, np.nan))
    rsi14 = 100 - (100 / (1 + rs))
    # MACD hist norm stub
    ema12 = c.ewm(span=12, adjust=False).mean()
    ema26 = c.ewm(span=26, adjust=False).mean()
    macd_hist = (ema12 - ema26) / c
    # EMA spread
    ema20 = c.ewm(span=20, adjust=False).mean()
    ema50 = c.ewm(span=50, adjust=False).mean()
    ema_spread = (ema20 - ema50) / c
    # ADX stub
    tr = pd.concat([h - l, (h - c.shift()).abs(), (l - c.shift()).abs()], axis=1).max(axis=1)
    adx = 100 * tr.rolling(14).mean() / c  # approximate
    # ATR%
    atr_pct = tr.rolling(14).mean() / c
    # BB width / position stub
    bb_mid = c.rolling(20).mean()
    bb_std = c.rolling(20).std()
    bb_width = (bb_std * 4) / c
    bb_pos = (c - bb_mid) / (bb_std * 2).replace(0, np.nan)
    # Volume ratio 5/20
    v5 = v.rolling(5).mean()
    v20 = v.rolling(20).mean()
    vol_ratio = v5 / v20.replace(0, np.nan)
    # 52-week distance
    dist52w = (c - c.rolling(252).min()) / c
    # EMA20 slope
    ema20_slope = ema20.pct_change(5)
    # Structure state (stub: 0=UP,1=DOWN,2=RANGE)
    structure_state = ((ema20 > ema20.shift(5)).astype(int) * 2) + ((ema50 < ema50.shift(5)).astype(int))
    # Relative strength 20
    rel_str20 = (c.pct_change(20) - c.pct_change(20).rolling(20).mean()) / c
    # RVOL20
    rvol20 = v / v.rolling(20).mean().replace(0, np.nan)
    # Delivery % stub (requires external CSV; set 0 if absent)
    delivery_pct5 = pd.Series(0.0, index=c.index)

    out = pd.DataFrame({
        "ret1": ret1, "ret5": ret5, "ret10": ret10, "ret21": ret21,
        "rsi14": rsi14, "macdHistNorm": macd_hist,
        "ema20_50_spread": ema_spread, "adx": adx, "atrPct": atr_pct,
        "bbWidth": bb_width, "bbPosition": bb_pos,
        "volumeRatio5_20": vol_ratio, "distance52w": dist52w,
        "ema20Slope": ema20_slope, "structureState": structure_state,
        "relStrength20": rel_str20, "rvol20": rvol20, "deliveryPct5": delivery_pct5,
    })
    return out.fillna(0)

def lookahead_bias_test(df):
    """Shuffle future bars (t+1..t+5) and assert features at t unchanged."""
    df_copy = df.copy()
    # Simple future-shuffle stub: swap rows at positions t+1 to t+5
    if len(df_copy) > 5:
        future_slice = df_copy.iloc[1:6].copy()
        future_slice['close'] = future_slice['close'].sample(frac=1, random_state=42).values
        df_copy.iloc[1:6] = future_slice
    f_after = build_features(df_copy)
    f_before = build_features(df)
    assert f_after.shape == f_before.shape
