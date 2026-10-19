"""Technical indicators — point-in-time, no TA-Lib."""
from __future__ import annotations
import numpy as np, pandas as pd


def rsi(close: np.ndarray, period: int = 14) -> np.ndarray:
    delta = np.diff(np.concatenate([[close[0]], close]))
    gain = np.clip(delta, 0, None)
    loss = np.clip(-delta, 0, None)
    avg_gain = pd.Series(gain).rolling(period, min_periods=period).mean().to_numpy()
    avg_loss = pd.Series(loss).rolling(period, min_periods=period).mean().to_numpy()
    rs = avg_gain / (avg_loss + 1e-9)
    return np.concatenate([[np.nan], 100 - (100 / (1 + rs))])[-len(close):]


def rsi_divergence(close: np.ndarray, rsi_vals: np.ndarray, lookback: int = 40) -> tuple[bool, bool]:
    if len(close) < lookback + 10:
        return False, False
    pivs_low, pivs_high = _find_pivots(close[-lookback:])
    r_low, r_high = _find_pivots(rsi_vals[-lookback:])
    bearish = (len(pivs_high) >= 2 and close[pivs_high[-1]] > close[pivs_high[-2]] and
               rsi_vals[pivs_high[-1]] < rsi_vals[pivs_high[-2]])
    bullish = (len(pivs_low) >= 2 and close[pivs_low[-1]] < close[pivs_low[-2]] and
               rsi_vals[pivs_low[-1]] > rsi_vals[pivs_low[-2]])
    return bullish, bearish


def _find_pivots(arr: np.ndarray) -> tuple[list[int], list[int]]:
    from scipy.signal import find_peaks
    lows, _ = find_peaks(-arr, distance=3)
    highs, _ = find_peaks(arr, distance=3)
    return list(lows), list(highs)


def macd(close: np.ndarray, fast: int = 12, slow: int = 26, sig: int = 9) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    ema_fast = pd.Series(close).ewm(span=fast, adjust=False).mean().to_numpy()
    ema_slow = pd.Series(close).ewm(span=slow, adjust=False).mean().to_numpy()
    line = ema_fast - ema_slow
    signal = pd.Series(line).ewm(span=sig, adjust=False).mean().to_numpy()
    hist = line - signal
    return line, signal, hist


def ema_set(close: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    return (pd.Series(close).ewm(span=20, adjust=False).mean().to_numpy(),
            pd.Series(close).ewm(span=50, adjust=False).mean().to_numpy(),
            pd.Series(close).ewm(span=200, adjust=False).mean().to_numpy())


def adx_di(high: np.ndarray, low: np.ndarray, close: np.ndarray, period: int = 14) -> tuple[float, float, float]:
    close_shift = np.roll(close, 1)
    close_shift[0] = close[0]
    tr = np.maximum(high - low, np.maximum(np.abs(high - close_shift), np.abs(low - close_shift)))
    atr_s = pd.Series(tr).rolling(period, min_periods=period).mean().to_numpy()
    up = high - np.roll(high, 1)
    down = np.roll(low, 1) - low
    up = np.clip(up, 0, None)
    down = np.clip(down, 0, None)
    dm_up = np.where(up > down, up, 0)
    dm_down = np.where(down > up, down, 0)
    plus_di = 100 * (pd.Series(dm_up).rolling(period, min_periods=period).mean().to_numpy() / (atr_s + 1e-9))
    minus_di = 100 * (pd.Series(dm_down).rolling(period, min_periods=period).mean().to_numpy() / (atr_s + 1e-9))
    dx = 100 * np.abs(plus_di - minus_di) / (plus_di + minus_di + 1e-9)
    adx_s = pd.Series(dx).rolling(period, min_periods=period).mean().to_numpy()
    return float(adx_s[-1]), float(plus_di[-1]), float(minus_di[-1])


def atr_pct(high: np.ndarray, low: np.ndarray, close: np.ndarray, period: int = 14) -> float:
    close_shift = np.roll(close, 1)
    close_shift[0] = close[0]
    tr = np.maximum(high - low, np.maximum(np.abs(high - close_shift), np.abs(low - close_shift)))
    atr_s = pd.Series(tr).rolling(period, min_periods=period).mean()
    return float(atr_s.iloc[-1] / close[-1] * 100)


def bollinger_band(close: np.ndarray, period: int = 20, std: int = 2) -> tuple[float, float, float, float, bool]:
    s = pd.Series(close)
    ma = s.rolling(period, min_periods=period).mean().to_numpy()
    std_s = s.rolling(period, min_periods=period).std().to_numpy()
    upper = ma[-1] + std * std_s[-1]
    lower = ma[-1] - std * std_s[-1]
    width = float((upper - lower) / ma[-1])
    position = float((close[-1] - lower) / (upper - lower + 1e-9))
    # Width series is recomputed below (correctly) to derive the squeeze flag.
    ma_series = s.rolling(period, min_periods=period).mean()
    std_series = s.rolling(period, min_periods=period).std()
    upper_series = ma_series + std * std_series
    lower_series = ma_series - std * std_series
    width_series = ((upper_series - lower_series) / ma_series).to_numpy()
    median_120 = float(np.median(width_series[-120:] if len(width_series) >= 120 else width_series))
    squeeze_flag = width < 0.5 * median_120
    return float(upper_series.iloc[-1]), float(lower_series.iloc[-1]), width, position, squeeze_flag


def obv_slope(close: np.ndarray, volume: np.ndarray) -> float:
    obv = np.where(np.diff(np.concatenate([[close[0]], close])) > 0, volume[1:], np.where(np.diff(np.concatenate([[close[0]], close])) < 0, -volume[1:], 0))
    # Correct OBV
    change = np.diff(close)
    obv_vals = np.zeros(len(close))
    obv_vals[0] = volume[0]
    for i in range(1, len(close)):
        if change[i-1] > 0:
            obv_vals[i] = obv_vals[i-1] + volume[i]
        elif change[i-1] < 0:
            obv_vals[i] = obv_vals[i-1] - volume[i]
        else:
            obv_vals[i] = obv_vals[i-1]
    slope = float(np.polyfit(np.arange(len(obv_vals)), obv_vals, 1)[0] / np.mean(obv_vals[-20:])) if np.mean(obv_vals[-20:]) != 0 else 0.0
    return slope


def volume_ratio_5_20(volume: np.ndarray) -> float:
    if len(volume) < 20:
        return 1.0
    return float(np.mean(volume[-5:]) / (np.mean(volume[-20:-5]) + 1e-9))


def delivery_pct(csv_path: str | None) -> float | None:
    try:
        import csv
        with open(csv_path, newline="") as f:
            reader = csv.DictReader(f)
            values = [float(row.get("delivery_pct", row.get("delivery", 0))) for row in reader if row.get("delivery_pct", row.get("delivery"))]
        return float(np.mean(values[-5:])) if values else None
    except Exception:
        return None
