"""Search endpoint for stock symbols.

Provides prefix+substring search functionality with exact prefix boosting.
"""
from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List, Optional
import json
from pathlib import Path

router = APIRouter()

class SearchResult(BaseModel):
    """Search result model."""
    symbol: str
    name: str
    sector: str


@router.get("/search", response_model=List[SearchResult])
def search(q: Optional[str] = Query(None, description="Search query")):
    """Search for stock symbols with prefix and substring matching."""
    symbols_file = Path(__file__).resolve().parents[1] / "data" / "symbols.json"
    if not symbols_file.exists():
        return []

    try:
        with open(symbols_file, "r") as f:
            symbols = json.load(f)
    except (OSError, json.JSONDecodeError):
        return []

    results = []

    if q and q.strip():
        q_lower = q.strip().lower()
        scored_results = []

        for symbol in symbols:
            sym = symbol.get("symbol", "")
            name = symbol.get("name", "")
            sector = symbol.get("sector", "")

            sym_lower = sym.lower()
            name_lower = name.lower()

            # Exact prefix boost (highest score)
            if sym_lower.startswith(q_lower) or name_lower.startswith(q_lower):
                score = 100
            # Prefix match in symbol
            elif sym_lower.startswith(q_lower):
                score = 90
            # Substring match
            elif q_lower in sym_lower or q_lower in name_lower:
                score = 50
            else:
                continue

            scored_results.append((score, symbol))

        # Sort by score (exact prefix first), then by symbol
        scored_results.sort(key=lambda x: (-x[0], x[1].get("symbol", "")))
        results = [s[1] for s in scored_results[:20]]
    else:
        # Return all symbols sorted by sector
        sorted_symbols = sorted(symbols, key=lambda x: (x.get("sector", ""), x.get("symbol", "")))
        results = sorted_symbols[:20]

    # Convert to response models
    return [
        SearchResult(
            symbol=r.get("symbol", ""),
            name=r.get("name", ""),
            sector=r.get("sector", "")
        )
        for r in results
    ]
