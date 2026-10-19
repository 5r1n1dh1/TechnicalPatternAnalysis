"""Test pattern detection with synthetic fixtures."""
import os
import pandas as pd
from core import patterns


def test_positive_fixtures():
    patterns_dir = "tests/fixtures/patterns"
    positive_files = [
        "hs_pos.csv",
        "ihs_pos.csv",
        "cup_pos.csv",
        "double_top_pos.csv",
        "double_bottom_pos.csv",
        "triple_top_pos.csv",
        "triple_bottom_pos.csv",
        "bull_flag_pos.csv",
        "bear_flag_pos.csv",
    ]
    for fname in positive_files:
        fpath = os.path.join(patterns_dir, fname)
        if not os.path.exists(fpath):
            continue
        df = pd.read_csv(fpath)
        detections = patterns.detect_patterns(df)
        assert len(detections) >= 1, f"{fname} should have >=1 detection"
        assert any(d.confidence >= 70 for d in detections), f"{fname} should have conf >=70"


def test_negative_fixtures():
    patterns_dir = "tests/fixtures/patterns"
    negative_files = [
        "hs_neg.csv",
        "cup_neg.csv",
        "double_neg.csv",
        "flag_neg.csv",
        "noisy_flat.csv",
    ]
    for fname in negative_files:
        fpath = os.path.join(patterns_dir, fname)
        if not os.path.exists(fpath):
            continue
        df = pd.read_csv(fpath)
        detections = patterns.detect_patterns(df)
        assert len(detections) == 0, f"{fname} should have 0 detections, got {len(detections)}"
