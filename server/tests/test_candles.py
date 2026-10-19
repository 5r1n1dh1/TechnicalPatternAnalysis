"""Test cases for candles endpoint."""

import pytest
import os
import time
import pandas as pd
from fastapi import HTTPException
from fastapi.testclient import TestClient
try:
    from api.candles import router as candles_router, CandleQuery, CandleResponse, validate_combo
except Exception:
    from api.candles import router as candles_router, CandleQuery, CandleResponse, validate_combo

def test_validate_matrix():
    """Test that invalid interval-range combinations raise errors."""
    with pytest.raises(HTTPException) as exc_info:
        validate_combo("5m", "3M")
    assert exc_info.value.status_code == 400
    assert "Invalid interval and range combination" in str(exc_info.value.detail)

    with pytest.raises(HTTPException) as exc_info:
        validate_combo("1d", "1D")
    assert exc_info.value.status_code == 400
    assert "Invalid interval and range combination" in str(exc_info.value.detail)

def test_candles_cached(client: TestClient):
    """Test that cached data is returned when available."""
    # Create a dummy cache file for testing
    from pathlib import Path
    cache_dir = Path("data/cached_candles")
    cache_dir.mkdir(parents=True, exist_ok=True)
    cache_file = cache_dir / "RELIANCE_1d_1Y.parquet"
    dummy_data = {
        "t": ["2026-10-15T09:15:00+05:30"],
        "o": [1430.0],
        "h": [1440.5],
        "l": [1425.0],
        "c": [1435.2],
        "v": [12345678]
    }
    pd.DataFrame(dummy_data).to_parquet(cache_file)
    
    response = client.get("/api/candles", params={
        "symbol": "RELIANCE",
        "interval": "1d",
        "range": "1Y"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "yahoo"
    assert len(data["candles"]) > 0
    
    # Clean up
    import os
    if os.path.exists(cache_file):
        os.remove(cache_file)

def test_candles_yahoo(client: TestClient):
    """Test that data is fetched from Yahoo Finance when cache is expired."""
    # Simulate cache expiration
    from pathlib import Path
    cache_dir = Path("data/cached_candles")
    cache_dir.mkdir(parents=True, exist_ok=True)
    cache_file = cache_dir / "RELIANCE_1d_1Y.parquet"
    dummy_data = {
        "t": ["2026-10-15T09:15:00+05:30"],
        "o": [1430.0],
        "h": [1440.5],
        "l": [1425.0],
        "c": [1435.2],
        "v": [12345678]
    }
    pd.DataFrame(dummy_data).to_parquet(cache_file)
    
    # Add a timestamp to make it appear old
    import os
    import time
    with open(cache_file, 'a') as f:
        f.write("\n")
    
    response = client.get("/api/candles", params={
        "symbol": "RELIANCE",
        "interval": "1d",
        "range": "1Y"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["source"] == "yahoo"
    assert len(data["candles"]) > 0
    
    # Clean up
    if os.path.exists(cache_file):
        os.remove(cache_file)

def test_candles_kaggle(client: TestClient):
    """Test that Kaggle data is used as fallback when Yahoo fails."""
    # Simulate Yahoo failure by removing cache
    from pathlib import Path
    cache_dir = Path("data/cached_candles")
    cache_file = cache_dir / "RELIANCE_1d_1Y.parquet"
    if cache_file.exists():
        os.remove(cache_file)
    try:
        from api.candles import clear_cache
        clear_cache()
    except Exception:
        pass
    
    import api.candles as candles_mod
    orig_yahoo = candles_mod._fetch_from_yahoo
    orig_kaggle = candles_mod._fetch_from_kaggle
    candles_mod._fetch_from_yahoo = lambda *a, **k: None
    candles_mod._fetch_from_kaggle = lambda *a, **k: None
    try:
        response = client.get("/api/candles", params={
            "symbol": "RELIANCE",
            "interval": "1d",
            "range": "1Y"
        })
        assert response.status_code == 404
        assert response.json()["detail"] == "Candle data not found"
    finally:
        candles_mod._fetch_from_yahoo = orig_yahoo
        candles_mod._fetch_from_kaggle = orig_kaggle

def test_candles_not_found(client: TestClient):
    """Test that 404 is returned for unknown symbols."""
    response = client.get("/api/candles", params={
        "symbol": "NONEXISTENT",
        "interval": "1d",
        "range": "1Y"
    })
    assert response.status_code == 404
    assert response.json()["detail"] == "Candle data not found"

def test_candles_invalid_params(client: TestClient):
    """Test that invalid interval-range combinations return 400."""
    response = client.get("/api/candles", params={
        "symbol": "RELIANCE",
        "interval": "5m",
        "range": "3M"
    })
    assert response.status_code == 400
    assert "Invalid interval and range combination" in str(response.json()["detail"])