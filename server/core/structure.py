"""Market structure engine per book-spec §5.1."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import List, Optional

import numpy as np
import pandas as pd
from scipy.signal import find_peaks


class StructureState(str, Enum):
    """Market structure state."""
    UP = "UP"
    DOWN = "DOWN"
    RANGE = "RANGE"
    CHOCH_UP = "CHOCH_UP"
    CHOCH_DOWN = "CHOCH_DOWN"
    MSS_UP = "MSS_UP"
    MSS_DOWN = "MSS_DOWN"


def atr(high: np.ndarray, low: np.ndarray, close: np.ndarray, period: int = 14) -> np.ndarray:
    """Compute Average True Range."""
    prev_close = np.roll(close, 1)
    prev_close[0] = close[0]
    tr = np.maximum(high - low, np.maximum(np.abs(high - prev_close), np.abs(low - prev_close)))
    return pd.Series(tr).rolling(period).mean().to_numpy()


def sma(data: np.ndarray, period: int) -> np.ndarray:
    """Simple moving average."""
    return pd.Series(data).rolling(period).mean().to_numpy()


def detect_structure(candles: pd.DataFrame, lookback: int = 150) -> StructureState:
    """Detect market structure per book-spec §5.1.

    - Swing highs/lows via find_peaks on close (window 5).
    - Uptrend = HH+HL sequence (last 3); Down = LH+LL; else RANGE.
    - BOS = close beyond last confirmed swing in trend direction.
    - ChoCH = close beyond last confirmed swing against trend.
    - MSS = ChoCH + impulsive displacement candle (range >= 1.5*ATR).

    Args:
        candles: DataFrame with columns open, high, low, close, volume
        lookback: Number of bars to analyze (default 150)

    Returns:
        StructureState enum value.
    """
    if len(candles) < lookback:
        lookback = len(candles)
    if lookback < 20:
        return StructureState.RANGE

    recent = candles.tail(lookback).copy()
    close = recent["close"].to_numpy()
    high = recent["high"].to_numpy()
    low = recent["low"].to_numpy()

    # Compute ATR
    atr_vals = atr(high, low, close, period=14)

    # Find swing highs/lows on close with window 5
    # Use find_peaks with distance=5 to approximate window
    peaks, _ = find_peaks(close, distance=5, prominence=atr_vals[-1] * 0.5)
    troughs, _ = find_peaks(-close, distance=5, prominence=atr_vals[-1] * 0.5)

    if len(peaks) < 2 or len(troughs) < 2:
        return StructureState.RANGE

    # Get last 3 swing highs and lows
    last_peaks = peaks[-3:]
    last_troughs = troughs[-3:]

    # Check for uptrend: HH + HL sequence (last 3)
    is_uptrend = False
    if len(last_peaks) >= 2 and len(last_troughs) >= 2:
        hh = close[last_peaks[-1]] > close[last_peaks[-2]]
        hl = close[last_troughs[-1]] > close[last_troughs[-2]]
        if len(last_peaks) >= 3 and len(last_troughs) >= 3:
            hh_prev = close[last_peaks[-2]] > close[last_peaks[-3]]
            hl_prev = close[last_troughs[-2]] > close[last_troughs[-3]]
            is_uptrend = hh and hl and hh_prev and hl_prev
        else:
            is_uptrend = hh and hl

    # Check for downtrend: LH + LL sequence (last 3)
    is_downtrend = False
    if len(last_peaks) >= 2 and len(last_troughs) >= 2:
        lh = close[last_peaks[-1]] < close[last_peaks[-2]]
        ll = close[last_troughs[-1]] < close[last_troughs[-2]]
        if len(last_peaks) >= 3 and len(last_troughs) >= 3:
            lh_prev = close[last_peaks[-2]] < close[last_peaks[-3]]
            ll_prev = close[last_troughs[-2]] < close[last_troughs[-3]]
            is_downtrend = lh and ll and lh_prev and ll_prev
        else:
            is_downtrend = lh and ll

    if is_uptrend:
        # Check for BOS (continuation) or ChoCH (reversal)
        last_swing_high_idx = last_peaks[-1]
        last_swing_low_idx = last_troughs[-1]
        last_close = close[-1]

        # BOS = close beyond last confirmed swing high in uptrend
        if last_close > close[last_swing_high_idx]:
            return StructureState.UP  # continuation
        # ChoCH = close beyond last swing low (against trend)
        elif last_close < close[last_swing_low_idx]:
            # Check for MSS: ChoCH + impulsive displacement candle
            candle_range = high[-1] - low[-1]
            if candle_range >= 1.5 * atr_vals[-1]:
                return StructureState.MSS_DOWN
            return StructureState.CHOCH_DOWN
        return StructureState.UP

    elif is_downtrend:
        # Check for BOS (continuation) or ChoCH (reversal)
        last_swing_high_idx = last_peaks[-1]
        last_swing_low_idx = last_troughs[-1]
        last_close = close[-1]

        # BOS = close beyond last swing low in downtrend
        if last_close < close[last_swing_low_idx]:
            return StructureState.DOWN  # continuation
        # ChoCH = close beyond last swing high (against trend)
        elif last_close > close[last_swing_high_idx]:
            # Check for MSS
            candle_range = high[-1] - low[-1]
            if candle_range >= 1.5 * atr_vals[-1]:
                return StructureState.MSS_UP
            return StructureState.CHOCH_UP
        return StructureState.DOWN

    return StructureState.RANGE