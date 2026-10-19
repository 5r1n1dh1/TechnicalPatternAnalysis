"""Cup & Handle pattern detector per book-spec §7."""

from __future__ import annotations

from typing import List, Optional

import pandas as pd
import numpy as np

from ..patterns import SwingPivot, Detection


class CupHandleDetector:
    """Detects Cup & Handle patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        """Detect cup and handle patterns.

        Args:
            pivots: List of SwingPivot objects
            candles: DataFrame with OHLCV data

        Returns:
            List of Detection objects.
        """
        if len(pivots) < 5:
            return []

        # Find low pivots to fit quadratic
        low_pivots = [p for p in pivots if not p.is_high]
        if len(low_pivots) < 5:
            return []

        # Fit quadratic to low pivots (cup shape)
        x = np.array([p.idx for p in low_pivots])
        y = np.array([p.price for p in low_pivots])

        # Fit quadratic y = ax^2 + bx + c
        coeffs = np.polyfit(x, y, 2)
        a, b, c = coeffs

        # Calculate R^2
        y_pred = np.polyval(coeffs, x)
        ss_res = np.sum((y - y_pred) ** 2)
        ss_tot = np.sum((y - np.mean(y)) ** 2)
        r_squared = 1 - (ss_res / ss_tot) if ss_tot > 0 else 0

        # Cup must have R^2 >= 0.8 and be U-shaped (a > 0)
        if r_squared < 0.8 or a <= 0:
            return []

        # Cup depth: difference between rim high and cup low
        rim_price = y.max()
        cup_low = y.min()
        cup_depth = rim_price - cup_low

        # Handle: drift <= 1/3 cup depth below rim
        handle_depth_threshold = cup_depth / 3
        handle_low = candles["low"].min()  # approximate
        handle_depth = rim_price - handle_low

        if handle_depth > handle_depth_threshold:
            return []  # handle too deep

        # Handle duration: 5-25 bars
        handle_duration = len(candles)  # approximate
        if handle_duration < 5 or handle_duration > 25:
            return []

        # Breakout above rim with volume
        breakout_price = candles["close"].iloc[-1]
        breakout_volume = candles["volume"].iloc[-1]
        avg_volume = candles["volume"].mean()

        if breakout_price < rim_price:
            return []  # no breakout yet

        if breakout_volume < 1.5 * avg_volume:
            return []  # insufficient volume expansion

        # Measured move target = rim + cup depth
        target_price = rim_price + cup_depth

        # Stop = below handle low + 0.5 ATR
        atr_val = candles["atr"].iloc[-1] if "atr" in candles.columns else 0
        stop_price = handle_low - 0.5 * atr_val

        # R:R
        rr = (target_price - breakout_price) / (breakout_price - stop_price) if breakout_price > stop_price else 0

        return [Detection(
            pattern="cup_and_handle",
            direction="bullish",
            confidence=80.0,
            anchors=[{
                "t": str(int(low_pivots[0].idx)),
                "price": float(low_pivots[0].price)
            }, {
                "t": str(int(low_pivots[-1].idx)),
                "price": float(low_pivots[-1].price)
            }],
            neckline={"t": str(int(low_pivots[len(low_pivots)//2].idx)), "price": float(rim_price)},
            target_price=target_price,
            stop_price=stop_price,
            rr=rr,
            invalidation_note="Price breaks below handle low"
        )]
