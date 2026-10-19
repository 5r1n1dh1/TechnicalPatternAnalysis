"""Head and Shoulders pattern detector per book-spec §7."""

from __future__ import annotations

from typing import List, Optional

import pandas as pd
import numpy as np

from ..patterns import SwingPivot, Detection


class HeadShouldersDetector:
    """Detects bearish and bullish Head & Shoulders patterns."""

    def detect(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> List[Detection]:
        """Detect head and shoulder patterns.

        Args:
            pivots: List of SwingPivot objects
            candles: DataFrame with OHLCV data

        Returns:
            List of Detection objects.
        """
        # Need at least 5 pivots to form H&S
        if len(pivots) < 5:
            return []

        # Sort pivots by index (chronological)
        pivots_sorted = sorted(pivots, key=lambda p: p.idx)

        # Identify potential heads and shoulders
        # Head = peak with two lower highs between
        # Shoulder = peak with one lower high between
        # We look for patterns where:
        # - Last 3 pivots form the head (peak, trough, peak)
        # - Middle pivot is the shoulder (lower peak)
        # - Before that, a preceding peak forms the head

        detections = []

        # Scan for head patterns (bearish)
        for i in range(len(pivots_sorted) - 2):
            # Potential head: peak at i, trough at i+1, peak at i+2
            if (pivots_sorted[i].is_high and
                pivots_sorted[i + 1].is_high == False and  # trough
                pivots_sorted[i + 2].is_high and
                # Ensure the middle is actually a trough (lower than neighbors)
                pivots_sorted[i + 1].price < pivots_sorted[i].price and
                pivots_sorted[i + 2].price < pivots_sorted[i + 1].price):
                # Check for confirmation: subsequent peak higher than both
                if i + 3 < len(pivots_sorted):
                    if pivots_sorted[i + 3].is_high and pivots_sorted[i + 3].price > pivots_sorted[i].price:
                        # Valid head
                        detection = self._build_head_detection(
                            pivots_sorted[i:i + 3], candles
                        )
                        if detection:
                            detections.append(detection)

        # Scan for shoulder patterns (bullish)
        for i in range(len(pivots_sorted) - 2):
            # Shoulder: peak at i, trough at i+1, peak at i+2
            if (pivots_sorted[i].is_high and
                pivots_sorted[i + 1].is_high == False and  # trough
                pivots_sorted[i + 2].is_high and
                # Ensure the middle is actually a trough
                pivots_sorted[i + 1].price < pivots_sorted[i].price and
                pivots_sorted[i + 2].price < pivots_sorted[i + 1].price):
                # Check for confirmation: subsequent peak higher than both
                if i + 3 < len(pivots_sorted):
                    if pivots_sorted[i + 3].is_high and pivots_sorted[i + 3].price > pivots_sorted[i].price:
                        # Valid shoulder
                        detection = self._build_shoulder_detection(
                            pivots_sorted[i:i + 3], candles
                        )
                        if detection:
                            detections.append(detection)

        return detections

    def _build_head_detection(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        """Build detection for a bearish head pattern."""
        # Calculate pattern metrics
        head_price = pivots[-1].price
        neckline_price = pivots[-2].price  # second peak acts as neckline
        # Measure move: neckline +/- (head - neckline)
        measure_move = neckline_price + (head_price - neckline_price)

        # Stop: beyond neckline + 0.5 ATR
        stop_price = neckline_price + 0.5 * candles["atr"].iloc[-1]

        # R:R based on measured move
        rr = measure_move - head_price if measure_move > head_price else abs(head_price - neckline_price)

        return Detection(
            pattern="head_and_shoulders",
            direction="bearish",
            confidence=85.0,  # default confidence
            anchors=[{
                "t": str(pivots[-1].idx),
                "price": float(pivots[-1].price)
            }, {
                "t": str(pivots[-2].idx),
                "price": float(pivots[-2].price)
            }],
            neckline={"t": str(pivots[-2].idx), "price": float(pivots[-2].price)},
            target_price=measure_move,
            stop_price=stop_price,
            rr=rr,
            invalidation_note="Neckline recaptured or new extreme breaks pattern"
        )

    def _build_shoulder_detection(self, pivots: List[SwingPivot], candles: pd.DataFrame) -> Optional[Detection]:
        """Build detection for a bullish shoulder pattern."""
        # Similar structure to head detection but inverted
        shoulder_price = pivots[-1].price
        neckline_price = pivots[-2].price
        measure_move = neckline_price + (shoulder_price - neckline_price)

        stop_price = neckline_price - 0.5 * candles["atr"].iloc[-1]

        rr = measure_move - shoulder_price if measure_move > shoulder_price else abs(shoulder_price - neckline_price)

        return Detection(
            pattern="head_and_shoulders",
            direction="bullish",
            confidence=85.0,
            anchors=[{
                "t": str(pivots[-1].idx),
                "price": float(pivots[-1].price)
            }, {
                "t": str(pivots[-2].idx),
                "price": float(pivots[-2].price)
            }],
            neckline={"t": str(pivots[-2].idx), "price": float(pivots[-2].price)},
            target_price=measure_move,
            stop_price=stop_price,
            rr=rr,
            invalidation_note="Neckline recaptured or new extreme breaks pattern"
        )
