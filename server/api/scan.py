"""GET /api/scan — baked scan.json from pipeline."""
from fastapi import APIRouter
import json
from pathlib import Path

router = APIRouter()
_DATA = Path(__file__).resolve().parents[1] / "data" / "scan.json"

@router.get("/api/scan")
def get_scan(page: int = 1):
    if not _DATA.exists():
        return {"asOf": None, "hits": [], "page": page}
    raw = json.loads(_DATA.read_text(encoding="utf-8"))
    hits = raw.get("hits", [])
    # minimal pagination
    per_page = 10
    start = (page - 1) * per_page
    paged = hits[start:start + per_page]
    return {
        "asOf": raw.get("asOf"),
        "hits": paged,
        "page": page,
        "total": len(hits)
    }
