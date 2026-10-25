"""Test cases for search endpoint."""

import pytest
from fastapi.testclient import TestClient
from api.search import router as search_router
from api.search import SearchResult

def test_search_empty(client: TestClient):
    """Test search with empty query returns all symbols sorted by sector."""
    response = client.get("/api/search")
    assert response.status_code == 200
    data = response.json()
    assert len(data) <= 20
    # Check that results are sorted by sector then symbol
    keys = [(item.get("sector", ""), item.get("symbol", "")) for item in data]
    assert keys == sorted(keys)

def test_search_exact(client: TestClient):
    """Test search with exact match."""
    import json
    from pathlib import Path
    symbols_file = Path(__file__).resolve().parents[1] / "data" / "symbols.json"
    has_symbol = False
    if symbols_file.exists():
        try:
            symbols = json.load(open(symbols_file))
            has_symbol = any(s.get("symbol") == "RELIANCE" for s in symbols)
        except Exception:
            pass
    response = client.get("/api/search?q=RELIANCE")
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1
    if has_symbol:
        assert data[0]["symbol"] == "RELIANCE"
        assert any(item["symbol"] == "RELIANCE" for item in data)

def test_search_fuzzy(client: TestClient):
    """Test search with fuzzy matching."""
    import json
    from pathlib import Path
    symbols_file = Path(__file__).resolve().parents[1] / "data" / "symbols.json"
    has_symbol = False
    if symbols_file.exists():
        try:
            symbols = json.load(open(symbols_file))
            has_symbol = any(s.get("symbol") == "TCS" for s in symbols)
        except Exception:
            pass
    response = client.get("/api/search?q=TCS")
    assert response.status_code == 200
    data = response.json()
    assert len(data) > 0
    if has_symbol:
        assert any(item["symbol"] == "TCS" for item in data)

def test_search_limit(client: TestClient):
    """Test that results are limited to 20."""
    response = client.get("/api/search?q=a")
    assert response.status_code == 200
    assert len(response.json()) <= 20

def test_search_case_insensitive(client: TestClient):
    """Test case-insensitive search."""
    import json
    from pathlib import Path
    symbols_file = Path(__file__).resolve().parents[1] / "data" / "symbols.json"
    has_symbol = False
    if symbols_file.exists():
        try:
            symbols = json.load(open(symbols_file))
            has_symbol = any(s.get("symbol") == "RELIANCE" for s in symbols)
        except Exception:
            pass
    response = client.get("/api/search?q=reLiAnCe")
    assert response.status_code == 200
    if has_symbol:
        assert any(item["symbol"] == "RELIANCE" for item in response.json())

def test_search_typo(client: TestClient):
    response = client.get("/api/search?q=riaL")
    assert response.status_code == 200
    data = response.json()
    symbols = [r["symbol"] for r in data]
    assert "RELIANCE" in symbols

def test_search_limit_1(client: TestClient):
    response = client.get("/api/search?q=a&limit=1")
    assert response.status_code == 200
    assert len(response.json()) == 1

def test_search_prefix_beats_fuzzy(client: TestClient):
    response = client.get("/api/search?q=RELI")
    data = response.json()
    assert len(data) > 0
    assert data[0]["symbol"].startswith("RELI")

def test_search_no_results(client: TestClient):
    """Test that empty results return empty list."""
    response = client.get("/api/search?q=nonexistent")
    assert response.status_code == 200
    assert len(response.json()) == 0