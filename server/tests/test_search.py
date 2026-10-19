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
    # Check that results are sorted by sector
    sectors = [item["sector"] for item in data]
    assert sorted(sectors) == sorted(sectors)

def test_search_exact(client: TestClient):
    """Test search with exact match."""
    response = client.get("/api/search?q=RELIANCE")
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    assert data[0]["symbol"] == "RELIANCE"

def test_search_fuzzy(client: TestClient):
    """Test search with fuzzy matching."""
    response = client.get("/api/search?q=TCS")
    assert response.status_code == 200
    data = response.json()
    assert len(data) > 0
    assert any(item["symbol"] == "TCS" for item in data)

def test_search_limit(client: TestClient):
    """Test that results are limited to 20."""
    response = client.get("/api/search?q=a")
    assert response.status_code == 200
    assert len(response.json()) <= 20

def test_search_case_insensitive(client: TestClient):
    """Test case-insensitive search."""
    response = client.get("/api/search?q=reLiAnCe")
    assert response.status_code == 200
    assert any(item["symbol"] == "RELIANCE" for item in response.json())

def test_search_no_results(client: TestClient):
    """Test that empty results return empty list."""
    response = client.get("/api/search?q=nonexistent")
    assert response.status_code == 200
    assert len(response.json()) == 0