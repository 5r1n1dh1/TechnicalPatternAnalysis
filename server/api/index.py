"""Single Vercel entrypoint. No yfinance/pandas at module level (cold start)."""

import json
from pathlib import Path
from typing import Any, Optional

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from server.api.errors import ApiError, ErrorResponse

_DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def _data_as_of() -> Optional[str]:
    """Read dataAsOf from baked symbols.json if present; never crash on absence/corruption."""
    path = _DATA_DIR / "symbols.json"
    if not path.exists():
        return None
    try:
        raw: Any = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    if isinstance(raw, dict):
        as_of = raw.get("asOf")
        return str(as_of) if as_of is not None else None
    return None


def create_app() -> FastAPI:
    app = FastAPI(title="Technical Pattern Analysis API", version="0.1.0")

    @app.exception_handler(ApiError)
    async def api_error_handler(request: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(status_code=exc.status, content=exc.payload.model_dump())

    @app.exception_handler(Exception)
    async def unhandled_handler(request: Request, exc: Exception) -> JSONResponse:
        body = ErrorResponse.model_validate(
            {"error": {"code": "internalError", "message": "Unexpected server error"}}
        )
        return JSONResponse(status_code=500, content=body.model_dump())

    @app.get("/api/health", response_model=dict)
    async def health() -> dict:
        return {"status": "ok", "dataAsOf": _data_as_of()}
    
    # Mount routers
    from server.api import (
        candles,
        decision,
        fundamentals,
        macro,
        patterns,
        predictions,
        resample,
        scan,
        search,
        technicals,
        tradeplan,
        trend,
    )

    app.include_router(search.router, prefix="/api")
    app.include_router(candles.router, prefix="/api")
    app.include_router(resample.router, prefix="/api")
    app.include_router(decision.router, prefix="/api")
    app.include_router(predictions.router, prefix="/api")
    app.include_router(scan.router, prefix="/api")
    app.include_router(technicals.router, prefix="/api")
    app.include_router(patterns.router, prefix="/api")
    app.include_router(trend.router, prefix="/api")
    app.include_router(fundamentals.router, prefix="/api")
    app.include_router(tradeplan.router, prefix="/api")
    app.include_router(macro.router, prefix="/api")

    return app


app = create_app()
