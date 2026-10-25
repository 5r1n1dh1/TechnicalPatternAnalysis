import json
from pathlib import Path
from unittest.mock import patch, MagicMock

import pandas as pd
import pytest

# Make pipeline importable
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from pipeline.backfill import backfill_data, load_universe


def make_frame(symbol="RELIANCE", n=3):
    """Return a fake yfinance DataFrame for one symbol with n rows (valid OHLCV)."""
    base = 100.0
    rows = []
    dates = pd.date_range("2026-01-01", periods=n, freq="B")
    for i, d in enumerate(dates):
        close = base + i
        rows.append({
            "Open": base - 2.0 + i * 0.1,
            "High": base + 3.0 + i * 0.1,
            "Low": base - 3.0 + i * 0.1,
            "Close": close,
            "Volume": 1000 + i * 10,
        })
    df = pd.DataFrame(rows, index=dates)
    df.index.name = "Date"
    # yfinance may return MultiIndex columns; ensure flat
    if getattr(df.columns, "nlevels", 1) > 1:
        df.columns = df.columns.get_level_values(0)
    return df


def test_load_universe():
    """Universe file must contain NSE tickers and include ^NSEI."""
    tickers = load_universe()
    assert len(tickers) >= 400
    assert "^NSEI" in tickers
    # No US tickers allowed
    for t in tickers:
        assert t not in ("MSFT", "GOOGL", "AAPL", "TSLA")


def test_universe_symbol_schema():
    """Each entry must carry symbol/name/sector."""
    with open("server/data/symbols.json") as f:
        symbols = json.load(f)
    assert isinstance(symbols, list)
    for s in symbols[:5]:
        assert {"symbol", "name", "sector"} <= set(s.keys())


@patch("pipeline.backfill.load_universe", return_value=["^NSEI", "^BSESN"])
@patch("pipeline.backfill.yf.download", side_effect=[make_frame("^NSEI"), make_frame("^BSESN")])
@patch("pipeline.backfill.os.path.exists", return_value=False)
def test_backfill_schema_and_rows(mock_exists, mock_yf, mock_lu):
    """backfill_data returns rows with correct schema and SC_CODE."""
    df = backfill_data()

    required = ["SC_CODE", "Date", "open", "high", "low", "close", "volume",
                "adjOpen", "adjHigh", "adjLow", "adjClose"]
    missing = [c for c in required if c not in df.columns]
    assert not missing, f"Missing columns: {missing}"

    unique_sym = df["SC_CODE"].unique().tolist()
    assert set(unique_sym) == {"^NSEI", "^BSESN"}, f"Unexpected symbols: {unique_sym}"

    assert pd.api.types.is_float_dtype(df["open"])
    assert pd.api.types.is_int64_dtype(df["volume"])


@patch("pipeline.backfill.load_universe", return_value=["RELIANCE", "BADSYM"])
@patch("pipeline.backfill.yf.download", side_effect=[make_frame("RELIANCE"), Exception("not found")])
@patch("pipeline.backfill.os.path.exists", return_value=False)
def test_backfill_skip_bad_symbol(mock_exists, mock_yf, mock_lu):
    """A symbol raising exceptions is logged/skipped, good symbols still produce rows."""
    df = backfill_data()
    assert df["SC_CODE"].nunique() == 1
    assert df["SC_CODE"].iloc[0] == "RELIANCE"
    assert len(df) > 0


@patch("pipeline.backfill.load_universe", return_value=["^NSEI"])
@patch("pipeline.backfill.yf.download", side_effect=[make_frame("^NSEI")])
@patch("pipeline.backfill.os.path.exists", return_value=False)
def test_limit_flag(mock_exists, mock_yf, mock_lu):
    """limit parameter truncates the universe."""
    df = backfill_data(limit=1)
    assert df["SC_CODE"].nunique() == 1
    assert df["SC_CODE"].iloc[0] == "^NSEI"


@patch("pipeline.backfill.load_universe", return_value=["RELIANCE"])
@patch("pipeline.backfill.yf.download", side_effect=[make_frame("RELIANCE")])
@patch("pipeline.backfill.os.path.exists", return_value=True)
@patch("pipeline.backfill.pd.read_parquet",
       return_value=pd.DataFrame([
           {"SC_CODE": "RELIANCE", "Date": "2026-01-01",
            "open": 100, "high": 110, "low": 95, "close": 105, "volume": 1000,
            "adjOpen": 100, "adjHigh": 110, "adjLow": 95, "adjClose": 105}
       ]))
def test_merge_with_existing_history(mock_read, mock_exists, mock_yf, mock_lu):
    """Existing history file is merged and deduped by (SC_CODE, Date)."""
    df = backfill_data()
    # Base has 1 row; we add new rows for the same symbol with distinct dates.
    # After merge+dedup by (SC_CODE, Date) we should have the same count as new data
    # plus any non-duplicate base rows.
    assert len(df) >= 1
    assert "RELIANCE" in df["SC_CODE"].values
    # No duplicate (SC_CODE, Date) pairs should remain
    assert df.duplicated(subset=["SC_CODE", "Date"]).sum() == 0


def test_sc_code_uppercase_no_ns():
    """SC_CODE must be uppercase; .NS suffix not in SC_CODE."""
    with patch("pipeline.backfill.load_universe", return_value=["RELIANCE"]), \
         patch("pipeline.backfill.yf.download", side_effect=[make_frame("RELIANCE")]), \
         patch("pipeline.backfill.os.path.exists", return_value=False):
        df = backfill_data()
    assert df["SC_CODE"].iloc[0] == "RELIANCE"
    assert not df["SC_CODE"].iloc[0].endswith(".NS")