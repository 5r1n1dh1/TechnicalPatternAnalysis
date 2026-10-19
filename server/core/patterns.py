"""Main pattern detection module."""

from __future__ import annotations

from abc import abstractmethod
from dataclasses import dataclass, field
from typing import List, Optional, Dict, Any

import numpy as np
import pandas as pd
from scipy.signal import find_peaks

try:
    from .structure import atr
except Exception:
    def atr(high, low, close, period=14):
        prev_close = np.roll(close, 1)
        prev_close[0] = close[0] if len(close) > 0 else 0
        tr = np.maximum(high - low, np.maximum(np.abs(high - prev_close), np.abs(low - prev_close)))
        return pd.Series(tr).rolling(period).mean().to_numpy()


@dataclass
class SwingPivot:
    """Swing high/low pivot."""
    idx: int
    price: float
    is_high: bool
    atr_at_pivot: Optional[float] = None


def sma(data: np.ndarray, period: int) -> np.ndarray:
    """Simple moving average."""
    if len(data) == 0:
        return data
    result = np.zeros_like(data, dtype=float)
    for i in range(period - 1, len(data)):
        result[i] = np.mean(data[i - period + 1:i + 1])
    for i in range(period - 1):
        if i >= 0:
            result[i] = np.mean(data[:i + 1])
    return result


def sma5_smooth(data: np.ndarray) -> np.ndarray:
    """Apply SMA5 smoothing."""
    return sma(data, 5)


def calculate_confidence(factors: Dict[str, float], base: float = 70.0) -> float:
    """Calculate proper confidence math based on factors."""
    confidence = base
    for factor, weight in factors.items():
        confidence += weight
    # Clamp between 0 and 100
    return max(0.0, min(100.0, confidence))


def find_pivots(
    close: np.ndarray,
    high: np.ndarray,
    low: np.ndarray,
    atr: np.ndarray,
    prominence_mult: float = 1.5,
    use_smoothing: bool = True,
    window_size: Optional[int] = None,
) -> List[SwingPivot]:
    """Find swing pivots using scipy.signal.find_peaks with prominence = prominence_mult * ATR.

    Args:
        close: Close prices (numpy array)
        high: High prices
        low: Low prices
        atr: Average True Range (numpy array)
        prominence_mult: Multiplier for prominence (default 1.5)
        use_smoothing: Apply SMA5 smoothing
        window_size: Sliding window size

    Returns:
        List of SwingPivot objects in chronological order.
    """
    # Apply smoothing if requested
    if use_smoothing and len(high) > 4:
        high_smooth = sma5_smooth(high)
        low_smooth = sma5_smooth(low)
        close_smooth = sma5_smooth(close)
    else:
        high_smooth = high
        low_smooth = low
        close_smooth = close

    # Apply sliding window if specified
    if window_size is not None and window_size < len(high_smooth):
        start = len(high_smooth) - window_size
        high_smooth = high_smooth[start:]
        low_smooth = low_smooth[start:]
        close_smooth = close_smooth[start:]
        atr_window = atr[start:] if len(atr) > start else atr
        offset = start
    else:
        atr_window = atr
        offset = 0

    # Find peaks in high (swing highs)
    # Prominence calculation: prominence_mult * ATR
    min_prom = prominence_mult * atr_window
    # Handle case where atr_window has zeros or NaN
    min_prom = np.where((min_prom <= 0) | np.isnan(min_prom), prominence_mult * np.mean(atr_window[atr_window > 0]) if np.any(atr_window > 0) else prominence_mult, min_prom)

    high_peaks, properties = find_peaks(
        high_smooth,
        prominence=min_prom,
        width=1
    )
    high_pivots = [
        SwingPivot(idx=int(i) + offset, price=float(high[int(i) + offset]), is_high=True, atr_at_pivot=float(atr[int(i) + offset]) if i + offset < len(atr) else 0.0)
        for i in high_peaks
    ]

    # Find troughs in low (swing lows) - find peaks in negative low
    low_troughs, properties = find_peaks(
        -low_smooth,
        prominence=min_prom,
        width=1
    )
    low_pivots = [
        SwingPivot(idx=int(i) + offset, price=float(low[int(i) + offset]), is_high=False, atr_at_pivot=float(atr[int(i) + offset]) if i + offset < len(atr) else 0.0)
        for i in low_troughs
    ]

    # Combine and sort by index
    all_pivots = high_pivots + low_pivots
    all_pivots.sort(key=lambda p: p.idx)
    return all_pivots


@dataclass
class Detection:
    """Single pattern detection result."""
    pattern: str
    direction: str  # "bearish" | "bullish"
    confidence: float  # 0-100
    anchors: List[Dict[str, Any]]  # [{"t": str, "price": float}, ...]
    neckline: Dict[str, Any]  # {"t": str, "price": float}
    target_price: Optional[float] = None
    stop_price: Optional[float] = None
    rr: float = 0.0  # risk:reward ratio
    invalidation_note: str = ""


class PatternDetector:
    """Abstract base class for pattern detectors."""

    @abstractmethod
    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        """Detect patterns given pivots and candle data.

        Args:
            pivots: List of SwingPivot objects
            candles: DataFrame with OHLCV data

        Returns:
            List of Detection objects
        """
        raise NotImplementedError


class TrendValidityMixin:
    """Mixin for trend precondition validity."""

    def check_prior_trend(self, candles: pd.DataFrame, lookback: int = 50) -> str:
        """Check prior trend - should be established before pattern.

        Returns:
            'uptrend', 'downtrend', 'neutral'
        """
        if len(candles) < lookback:
            lookback = len(candles)
        if lookback < 20:
            return 'neutral'

        recent = candles.tail(lookback)
        close = recent['close'].values

        # Simple trend check: MA slope or higher highs/lows
        # For simplicity, use moving average direction
        ma20 = sma(close, 20)[-1]
        ma50 = sma(close, 50)[-1] if len(close) >= 50 else ma20

        if close[-1] > ma20 > ma50:
            return 'uptrend'
        elif close[-1] < ma20 < ma50:
            return 'downtrend'
        return 'neutral'

    def has_prior_trend(self, candles: pd.DataFrame, direction: str) -> bool:
        """Check if prior trend matches expected direction."""
        prior = self.check_prior_trend(candles)
        if direction == 'bullish':
            return prior == 'uptrend'
        elif direction == 'bearish':
            return prior == 'downtrend'
        return prior != 'neutral'


class VolumeValidityMixin:
    """Mixin for volume sequence validity."""

    def check_volume_sequence(self, candles: pd.DataFrame, pattern_span: slice = None) -> bool:
        """Check volume contraction inside pattern + expansion on break.

        Volume should contract during pattern formation and expand on breakout.
        """
        if 'volume' not in candles.columns:
            return True  # If no volume data, don't reject

        if len(candles) < 10:
            return True

        # Simple heuristic: volume should decrease during pattern, increase at end
        if pattern_span is not None:
            start, end = pattern_span.start, pattern_span.stop
        else:
            end = len(candles) - 1
            start = max(0, end - 30)

        vol = candles['volume'].values
        if end - start < 5:
            return True

        # Volume contraction inside (middle < ends relative)
        mid_idx = (start + end) // 2
        mid_vol = np.mean(vol[mid_idx:end]) if end > mid_idx else vol[mid_idx] if mid_idx < len(vol) else 0
        early_vol = np.mean(vol[start:mid_idx]) if mid_idx > start else vol[start] if start < len(vol) else 0
        late_vol = vol[end] if end < len(vol) else 0

        # Contraction: middle volume lower than early; expansion on break: late > avg
        contraction_ok = mid_vol <= early_vol * 1.2  # Allow some flexibility
        expansion_ok = late_vol >= np.mean(vol[start:end]) * 1.1 if end > start else True

        return contraction_ok and expansion_ok


def detect_patterns(candles: pd.DataFrame, **kwargs) -> List[Detection]:
    """Detect all patterns in candle data."""
    from .detectors.head_shoulders import HeadShouldersDetector
    from .detectors.cup_handle import CupHandleDetector
    from .detectors.double_triple import DoubleTripleDetector
    from .detectors.flag import FlagDetector

    all_detections = []
    detectors = [
        HeadShouldersDetector(),
        CupHandleDetector(),
        DoubleTripleDetector(),
        FlagDetector(),
    ]

    # Compute ATR if not present
    if 'atr' not in candles.columns:
        try:
            high = candles['high'].values
            low = candles['low'].values
            close = candles['close'].values
            atr_vals = atr(high, low, close, period=14)
            candles = candles.copy()
            candles['atr'] = atr_vals
        except Exception:
            pass

    # Find pivots
    try:
        high = candles['high'].values
        low = candles['low'].values
        close = candles['close'].values
        atr_vals = candles['atr'].values if 'atr' in candles.columns else np.ones_like(close)
        pivots = find_pivots(close, high, low, atr_vals, use_smoothing=True)
    except Exception:
        pivots = []

    for detector in detectors:
        try:
            detections = detector.detect(pivots, candles)
            all_detections.extend(detections)
        except Exception:
            continue

    return all_detections