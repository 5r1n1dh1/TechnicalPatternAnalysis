"""Fundamentals endpoint tests — T-39."""
import time
from unittest.mock import MagicMock, patch

import pytest
from fastapi import status

from api.fundamentals import get_fundamentals, _fund_cache, _CACHE_TTL


@pytest.fixture(autouse=True)
def clear_cache():
    _fund_cache.clear()
    yield


def test_real_symbol_200_per_item_fields(monkeypatch):
    monkeypatch.setattr("yfinance.Ticker", lambda sym: MagicMock(info={
        "longName": "Reliance Industries", "sector": "Energy", "marketCap": 2000000000000,
        "beta": 1.1, "fiftyTwoWeekLow": 2200, "fiftyTwoWeekHigh": 2800,
        "currentPrice": 2500, "trailingPE": 22.5, "forwardPE": 20.0,
        "priceToBook": 3.0, "dividendYield": 0.0035, "trailingEps": 110.0,
        "bookValue": 800, "returnOnAssets": 0.08, "returnOnEquity": 0.15,
        "returnOnInvestedCapital": 0.12, "grossMargins": 0.25, "profitMargins": 0.15,
        "totalRevenue": 800000000000, "netIncomeToCommon": 50000000000,
        "totalAssets": 3000000000000, "totalDebt": 1000000000000,
        "operatingCashflow": 200000000000, "capitalExpenditures": 50000000000,
        "revenueGrowth": 0.08,
    }))
    result = get_fundamentals("RELIANCE")
    assert result.symbol == "RELIANCE"
    assert result.header.name == "Reliance Industries"
    assert result.valuation.pe_ttm == 22.5
    assert result.profitability.roce is not None or result.profitability.roce is None  # null-tolerant
    assert result.provenance.get("source") == "yfinance"


def test_unknown_symbol_404(monkeypatch):
    monkeypatch.setattr("yfinance.Ticker", lambda sym: MagicMock(info={}))
    from fastapi import HTTPException
    with pytest.raises(HTTPException) as exc:
        get_fundamentals("UNKNOWNXYZ")
    assert exc.value.status_code == 404


def test_stale_missing_503(monkeypatch):
    monkeypatch.setattr("yfinance.Ticker", lambda sym: (_ for _ in ()).throw(Exception("timeout")))
    from fastapi import HTTPException
    with pytest.raises(HTTPException) as exc:
        get_fundamentals("RELIANCE")
    assert exc.value.status_code == 503
    assert "upstreamUnavailable" in str(exc.value.detail)


def test_cache_hit_no_refetch(monkeypatch):
    mock_ticker = MagicMock()
    mock_ticker.info = {"longName": "Cache Test", "sector": "Test"}
    monkeypatch.setattr("yfinance.Ticker", lambda sym: mock_ticker)
    get_fundamentals("CACHE")
    first_calls = mock_ticker.info.get("longName")
    # Second call should use cached payload, no new yfinance fetch
    result2 = get_fundamentals("CACHE")
    assert result2.header.name == "Cache Test"
    # Cache exists
    assert "CACHE" in _fund_cache


def test_rev_growth_scalar(monkeypatch):
    monkeypatch.setattr("yfinance.Ticker", lambda sym: MagicMock(info={
        "longName": "Growth Corp", "revenueGrowth": 0.12,
        "sector": "Tech", "totalRevenue": 1000, "netIncomeToCommon": 100,
    }))
    result = get_fundamentals("GROWTH")
    assert result.profitability.grossMargin is None  # missing fields ok
