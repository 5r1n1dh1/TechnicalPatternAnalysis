"""Search endpoint for stock symbols.

Provides fuzzy search functionality with exact match boosting.
"""

from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List, Optional
import json
import os
from fuzzywuzzy import process
import pandas as pd

router = APIRouter()

class SearchResult(BaseModel):
    """Search result model."""
    symbol: str
    name: str
    sector: str

class SearchQuery(BaseModel):
    """Search query parameters."""
    q: Optional[str] = None

@router.get("/search", response_model=List[SearchResult])
def search(q: Optional[str] = Query(None, description="Search prefix")):
    """Search for stock symbols with fuzzy matching."""
    # Load symbols from JSON
    symbols_file = "server/data/symbols.json"
    if not os.path.exists(symbols_file):
        return []
        
    with open(symbols_file, "r") as f:
        symbols = json.load(f)
    
    results = []
    
    if q and q.strip():
        # Fuzzy matching with exact match boosting
        candidates = []
        for symbol in symbols:
            symbol_str = f"{symbol['symbol']} {symbol['name']}"
            candidates.append((symbol_str, symbol))
        
        # Get top matches
        matches = process.extract(q, [s[0] for s in candidates], limit=20)
        
        for match in matches:
            for candidate in candidates:
                if match[0] == candidate[0]:
                    results.append(SearchResult(
                        symbol=candidate[1]['symbol'],
                        name=candidate[1]['name'],
                        sector=candidate[1]['sector']
                    ))
                    break
    else:
        # Return all symbols sorted by sector
        sorted_symbols = sorted(symbols, key=lambda x: x['sector'])
        for symbol in sorted_symbols:
            results.append(SearchResult(
                symbol=symbol['symbol'],
                name=symbol['name'],
                sector=symbol['sector']
            ))
    
    return results[:20]