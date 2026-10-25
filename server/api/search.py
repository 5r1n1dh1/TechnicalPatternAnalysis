"""Search endpoint for stock symbols."""
import difflib
import json
from pathlib import Path
from typing import List, Optional

from fastapi import APIRouter, Query
from pydantic import BaseModel

router = APIRouter()

class SearchResult(BaseModel):
    symbol: str
    name: str
    sector: str

_cached = None
_cached_path = None
_cached_mtime = None

def _load(path: Path):
    global _cached, _cached_path, _cached_mtime
    try:
        mtime = path.stat().st_mtime
    except Exception:
        mtime = None
    if _cached is not None and _cached_path == path and _cached_mtime == mtime:
        return _cached
    with open(path, "r", encoding="utf-8") as f:
        _cached = json.load(f)
    _cached_path = path
    _cached_mtime = mtime
    return _cached

@router.get("/search", response_model=List[SearchResult])
def search(q: Optional[str] = Query(None), limit: int = Query(20, ge=1, le=50)):
    symbols_file = Path(__file__).resolve().parents[1] / "data" / "symbols.json"
    symbols = _load(symbols_file) if symbols_file.exists() else []
    if not isinstance(symbols, list):
        return []
    results = []
    if q and q.strip():
        q_lower = q.strip().lower()
        scored = []
        for symbol in symbols:
            sym = symbol.get("symbol", "")
            name = symbol.get("name", "")
            sym_lower = sym.lower()
            name_lower = name.lower()
            if sym_lower.startswith(q_lower):
                score = 100
            elif name_lower.startswith(q_lower):
                score = 95
            elif q_lower in sym_lower or q_lower in name_lower:
                score = 60
            else:
                score = None
                if sym_lower and sym_lower[0] == q_lower[0] and len(q_lower) <= len(sym_lower):
                    r_sym = difflib.SequenceMatcher(None, q_lower, sym_lower).ratio()
                    if r_sym >= 0.5:
                        score = 40
                if score is None:
                    for w in name.upper().split():
                        w_lower = w.lower()
                        if w_lower and w_lower[0] == q_lower[0] and len(q_lower) <= len(w_lower):
                            if difflib.SequenceMatcher(None, q_lower, w_lower).ratio() >= 0.5:
                                score = 40
                                break
                if score is None:
                    continue
            scored.append((score, symbol))
        scored.sort(key=lambda x: (-x[0], x[1].get("symbol", "")))
        results = [s[1] for s in scored[:limit]]
    else:
        sorted_sym = sorted(symbols, key=lambda x: (x.get("sector", ""), x.get("symbol", "")))
        results = sorted_sym[:limit]
    return [SearchResult(symbol=r.get("symbol", ""), name=r.get("name", ""), sector=r.get("sector", "")) for r in results]
