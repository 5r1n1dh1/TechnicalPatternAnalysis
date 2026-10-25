"""Point-in-time features (§7.1). No future bars used."""
import numpy as np
import pandas as pd


FEATURE_NAMES = [
    "ret1",
    "ret5",
    "ret10",
    "ret21",
    "rsi14",
    "macdHistNorm",
    "ema20_50_spread",
    "ema50_200_spread",
    "adx",
    "atrPct",
    "bbWidth",
    "bbPosition",
    "volumeRatio5_20",
    "distance52w",
    "distance52wHighPct",
    "distance52wLowPct",
    "dist200dmaPct",
    "maAlignment",
    "ema20Slope",
    "structureState",
    "relStrength20",
    "rvol20",
    "deliveryPct5",
    "rsi_div_bear",
    "rsi_div_bull",
]


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """Construct features from OHLCV + volume only (no future bars)."""
    o, h, l, c, v = df["open"], df["high"], df["low"], df["close"], df["volume"]
    # Stubs derived only from past rows (point-in-time)
    ret1 = c.pct_change(1)
    ret5 = c.pct_change(5)
    ret10 = c.pct_change(10)
    ret21 = c.pct_change(21)
    # RSI14
    delta = c.diff()
    gain = delta.clip(lower=0).rolling(14).mean()
    loss = -delta.clip(upper=0).rolling(14).mean()
    rs = gain / (loss.replace(0, np.nan))
    rsi14 = 100 - (100 / (1 + rs))
    # MACD hist norm
    ema12 = c.ewm(span=12, adjust=False).mean()
    ema26 = c.ewm(span=26, adjust=False).mean()
    macd_hist = (ema12 - ema26) / c
    # EMAs
    ema20 = c.ewm(span=20, adjust=False).mean()
    ema50 = c.ewm(span=50, adjust=False).mean()
    ema200 = c.ewm(span=200, adjust=False).mean()
    ema20_50_spread = (ema20 - ema50) / c
    ema50_200_spread = (ema50 - ema200) / c
    # ADX (Wilder smoothing) — proper 14-period directional movement
    tr = pd.concat([h - l, (h - c.shift()).abs(), (l - c.shift()).abs()], axis=1).max(axis=1)
    plus_dm = pd.Series(np.where((h - h.shift()) > (l.shift() - l), np.maximum(h - h.shift(), 0), 0), index=h.index)
    minus_dm = pd.Series(np.where((l.shift() - l) > (h - h.shift()), np.maximum(l.shift() - l, 0), 0), index=h.index)
    plus_di = 100 * (plus_dm.rolling(14, min_periods=14).mean()) / (tr.rolling(14, min_periods=14).mean() + 1e-6)
    minus_di = 100 * (minus_dm.rolling(14, min_periods=14).mean()) / (tr.rolling(14, min_periods=14).mean() + 1e-6)
    dx = 100 * (plus_di - minus_di).abs() / (plus_di + minus_di + 1e-6)
    adx = dx.rolling(14, min_periods=14).mean()
    # ATR%
    atr_pct = tr.rolling(14).mean() / c
    # BB
    bb_mid = c.rolling(20).mean()
    bb_std = c.rolling(20).std()
    bb_width = (bb_std * 4) / c
    bb_pos = (c - bb_mid) / (bb_std * 2).replace(0, np.nan)
    # Volume ratio
    v5 = v.rolling(5).mean()
    v20 = v.rolling(20).mean()
    vol_ratio = v5 / v20.replace(0, np.nan)
    # 52-week
    high52w = c.rolling(252).max()
    low52w = c.rolling(252).min()
    dist52w = (c - low52w) / c if False else (c / high52w - 1.0) * (-1.0)  # approximate
    # simpler forms as per intent
    distance52w = (high52w - c) / high52w.replace(0, np.nan)
    distance52wHighPct = (high52w - c) / high52w.replace(0, np.nan) * 100.0
    distance52wLowPct = (c - low52w) / low52w.replace(0, np.nan) * 100.0
    # dist from 200 dma pct
    dist200dmaPct = (c - ema200) / ema200.replace(0, np.nan) * 100.0
    # ma alignment
    ma_align = (ema50 > ema200).astype(float) + (ema20 > ema50).astype(float)
    maAlignment = ma_align
    # slope
    ema20_slope = ema20.pct_change(5)
    # structure
    structure_state = ((ema20 > ema20.shift(5)).astype(int) * 2) + ((ema50 < ema50.shift(5)).astype(int))
    # rel strength
    rel_str20 = (c.pct_change(20) - c.pct_change(20).rolling(20).mean()) / c
    # rvol
    rvol20 = v / v.rolling(20).mean().replace(0, np.nan)
    # delivery pct — rolling 5-day mean of the NSE delivery % column (may be NaN if unavailable)
    # The input df must contain a "deliveryPct" column when available (from pipeline/feature timestamp).
    # If missing from input, emit NaN (not fake 0.0 — M6 fix).
    if "deliveryPct" in df.columns:
        delivery_pct5 = df["deliveryPct"].rolling(5, min_periods=1).mean()
    else:
        delivery_pct5 = pd.Series(np.nan, index=c.index)
    # RSI divergence flags — compare RSI slope vs price slope over 5 bars (no lookahead)
    # Bullish divergence: price slope negative but RSI slope positive (potential reversal up)
    # Bearish divergence: price slope positive but RSI slope negative (potential reversal down)
    # If RSI is unavailable (NaN at warm-up), keep NaN (not 0.0 — M6 fix).
    try:
        rsi_5 = rsi14.rolling(5, min_periods=5).mean()
        rsi_slope_5 = rsi_5.diff()
    except Exception:
        rsi_slope_5 = pd.Series(np.nan, index=c.index)
    price_slope_5 = c.pct_change(5)
    rsi_div_bear = pd.Series(np.where(price_slope_5 > 0, np.where(rsi_slope_5 < 0, 1.0, np.nan), np.nan), index=c.index)
    rsi_div_bull = pd.Series(np.where(price_slope_5 < 0, np.where(rsi_slope_5 > 0, 1.0, np.nan), np.nan), index=c.index)

    out = pd.DataFrame(
        {
            "ret1": ret1,
            "ret5": ret5,
            "ret10": ret10,
            "ret21": ret21,
            "rsi14": rsi14,
            "macdHistNorm": macd_hist,
            "ema20_50_spread": ema20_50_spread,
            "ema50_200_spread": ema50_200_spread,
            "adx": adx,
            "atrPct": atr_pct,
            "bbWidth": bb_width,
            "bbPosition": bb_pos,
            "volumeRatio5_20": vol_ratio,
            "distance52w": distance52w,
            "distance52wHighPct": distance52wHighPct,
            "distance52wLowPct": distance52wLowPct,
            "dist200dmaPct": dist200dmaPct,
            "maAlignment": maAlignment,
            "ema20Slope": ema20_slope,
            "structureState": structure_state,
            "relStrength20": rel_str20,
            "rvol20": rvol20,
            "deliveryPct5": delivery_pct5,
            "rsi_div_bear": rsi_div_bear,
            "rsi_div_bull": rsi_div_bull,
        }
    )
    # Do NOT fill warm-up NaNs with 0 — they must remain NaN (M7 fix).
    # Callers (train, predict) must dropna on rows where any critical feature is NaN.
    # We keep the full frame intact so that feature order is preserved.
    return out


def lookahead_bias_test(df):
    """Shuffle future bars (t+1..) and assert features at t unchanged (no lookahead)."""
    df_copy = df.copy()
    if len(df_copy) > 6:
        # Shuffle a slice of future rows
        idx = df_copy.index
        # swap last N-1 rows randomly affects? better to permute from pos 1 onward
        future = df_copy.iloc[1:6].copy()
        rng = np.random.default_rng(42)
        perm = rng.permutation(len(future))
        future_shuf = future.iloc[perm].copy()
        df_copy.iloc[1:6] = future_shuf.values
    f_before = build_features(df)
    f_after = build_features(df_copy)
    assert f_after.shape == f_before.shape
    # features at index 0 must be unchanged
    if len(f_before) > 0:
        assert np.allclose(f_before.iloc[0].values, f_after.iloc[0].values, atol=1e-10), "Lookahead bias detected"
    return True
