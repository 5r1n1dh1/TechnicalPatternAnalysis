"""Tests for the baked EOD candle reader (server/core/baked.py).

No network: a tiny parquet is written into tmp_path and the reader is pointed
at it via the module-level ``_PARQUET`` attribute. A final test checks the real
committed artifact when it exists and skips cleanly otherwise.
"""

import pandas as pd
import pytest

from core import baked

_COLUMNS = ["t", "o", "h", "l", "c", "v"]


def _write_parquet(path, symbol):
    df = pd.DataFrame(
        {
            "symbol": [symbol, symbol, symbol],
            "t": pd.to_datetime(["2026-10-20", "2026-10-21", "2026-10-22"]),
            "o": [1.0, 2.0, 3.0],
            "h": [2.0, 3.0, 4.0],
            "l": [0.5, 1.5, 2.5],
            "c": [1.5, 2.5, 3.5],
            "v": [10.0, 20.0, 30.0],
        }
    )
    target = path / "candles.parquet"
    df.to_parquet(target, index=False)
    return target


@pytest.fixture
def pointed(tmp_path, monkeypatch):
    target = _write_parquet(tmp_path, "RELIANCE")
    monkeypatch.setattr(baked, "_PARQUET", target)
    monkeypatch.setattr(baked, "_CACHE", {})
    return target


def test_load_baked_columns_and_sort(pointed):
    out = baked.load_baked("RELIANCE", "1Y")
    assert out is not None
    assert list(out.columns) == _COLUMNS
    assert out["t"].is_monotonic_increasing
    assert len(out) == 3


def test_load_baked_accepts_suffixed_symbol(pointed):
    out = baked.load_baked("RELIANCE.NS", "1Y")
    assert out is not None
    assert len(out) == 3


def test_unknown_symbol_returns_none(pointed):
    assert baked.load_baked("NONEXISTENT", "1Y") is None


def test_range_trimming(pointed):
    assert len(baked.load_baked("RELIANCE", "1D")) == 1
    assert len(baked.load_baked("RELIANCE", "10Y")) == 3


def test_missing_file_returns_none(tmp_path, monkeypatch):
    monkeypatch.setattr(baked, "_PARQUET", tmp_path / "missing.parquet")
    monkeypatch.setattr(baked, "_CACHE", {})
    assert baked.load_baked("RELIANCE") is None


def test_real_artifact_known_symbol():
    """The committed dataset must be readable by the engine CI/Vercel installs
    (fastparquet is the only parquet engine in server/requirements.txt)."""
    path = baked._PARQUET
    if not path.exists():
        pytest.skip("baked parquet not built")
    out = baked.load_baked("RELIANCE", "1Y")
    assert out is not None, "committed candle dataset must be readable"
    assert list(out.columns) == _COLUMNS
    assert len(out) > 100
    assert out["t"].is_monotonic_increasing
