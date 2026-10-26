#!/usr/bin/env python3
"""Generate the bearish head-and-shoulders positive fixture.

Shape: prior uptrend -> left shoulder -> neckline trough -> head (highest)
-> neckline trough -> right shoulder (level with left shoulder) -> breakdown
through the neckline. The detector requires exactly this ordering, with the
head above both shoulders and the shoulders within 10% of each other.
"""
import csv
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "patterns" / "hs_pos.csv"

rows = [["date", "open", "high", "low", "close", "volume"]]
base_date = 20230101


def add(close, high, low, volume):
    rows.append(
        [
            str(base_date + len(rows) - 1),
            f"{close - 0.25:.2f}",
            f"{high:.2f}",
            f"{low:.2f}",
            f"{close:.2f}",
            str(volume),
        ]
    )


# Phase 1: prior uptrend into the left shoulder (high 111.5 at the last row)
for i in range(20):
    close = 101 + 0.5 * i
    add(close, close + 1.0, close - 2.0, 12000 + i * 150)

# Phase 2: pullback to the first neckline trough (low 100.8)
for i in range(8):
    close = 110 - 0.9 * (i + 1)
    add(close, close + 1.0, close - 2.0, 13500 - i * 250)

# Phase 3: rally to the head (high 117.3, above both shoulders)
for i in range(12):
    close = 103.8 + 1.0 * (i + 1)
    add(close, close + 1.5, close - 1.5, 11500 - i * 200)

# Phase 4: pullback to the second neckline trough (low 100.8)
for i in range(7):
    close = 115 - 1.7 * (i + 1)
    add(close, close + 0.8, close - 2.3, 9000 - i * 100)

# Phase 5: right shoulder rising to the left-shoulder level (high 111.4)
for i in range(8):
    close = 104 + 0.8 * (i + 1)
    add(close, close + 1.0, close - 1.0, 8200 + i * 100)

# Phase 5b: minor stall before the breakdown
add(109.5, 110.6, 108.6, 9000)

# Phase 6: breakdown through the neckline on expanding volume
for close, volume in zip(
    [108.0, 106.0, 103.5, 100.5, 97.5, 95.0],
    [18000, 21000, 24000, 28000, 32000, 36000],
):
    add(close, close + 0.8, close - 1.2, volume)

with open(OUT, "w", newline="") as f:
    csv.writer(f).writerows(rows)

print(f"Wrote {len(rows) - 1} rows to {OUT}")
