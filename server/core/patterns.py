"""Main pattern detection module."""

from __future__ import annotations

from abc import abstractmethod
from dataclasses import dataclass
from typing import List, Optional

import numpy as np
import pandas as pd
from pydantic import BaseModel
from scipy.signal import find_peaks


@dataclass
class SwingPivot:
    """Swing high/low pivot."""
    idx: int
    price: float
    is_high: bool


def find_pivots(
    close: np.ndarray,
    high: np.ndarray,
    low: np.ndarray,
    atr: np.ndarray,
    prominence_mult: float = 1.5,
) -> List[SwingPivot]:
    """Find swing pivots using scipy.signal.find_peaks with prominence = prominence_mult * ATR.

    Args:
        close: Close prices (numpy array)
        high: High prices
        low: Low prices
        atr: Average True Range (numpy array)
        prominence_mult: Multiplier for prominence (default 1.5)

    Returns:
        List of SwingPivot objects in chronological order.
    """
    # Find peaks in high (swing highs)
    # Prominence calculation: prominence_mult * ATR
    high_peaks, properties = find_peaks(
        high, 
        prominence=prominence_mult * atr,
        width=1
    )
    high_pivots = [
        SwingPivot(idx=int(i), price=float(high[i]), is_high=True)
        for i in high_peaks
    ]

    # Find troughs in low (swing lows) - find peaks in negative low
    low_troughs, properties = find_peaks(
        -low, 
        prominence=prominence_mult * atr,
        width=1
    )
    low_pivots = [
        SwingPivot(idx=int(i), price=float(low[i]), is_high=False)
        for i in low_troughs
    ]

    # Combine and sort by index
    all_pivots = high_pivots + low_pivots
    all_pivots.sort(key=lambda p: p.idx)
    return all_pivots


class Detection(BaseModel):
    """Single pattern detection result."""
    model_config = SettingsConfigDict()

    pattern: str
    direction: str  # "bearish" | "bullish"
    confidence: float  # 0-100
    anchors: List[dict]  # [{"t": str, "price": float}, ...]
    neckline: dict  # {"t": str, "price": float}
    target_price: Optional[float] = None
    stop_price: Optional[float] = None
    rr: float  # risk:reward ratio
    invalidation_note: str


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