"""Pydantic error models + helpers shared by all routers (camelCase, per C14)."""

from typing import Final

from pydantic import BaseModel


class ErrorBody(BaseModel):
    code: str
    message: str


class ErrorResponse(BaseModel):
    error: ErrorBody


class ApiError(Exception):
    """Carries an HTTP status + ErrorResponse payload; converted by the central handler."""

    def __init__(self, status: int, payload: ErrorResponse) -> None:
        self.status = status
        self.payload = payload
        super().__init__(payload.error.message)


_EOD_MSG: Final = "Data unavailable, try after today's EOD sync"


def unknown_symbol(symbol: str) -> ApiError:
    return ApiError(
        404,
        ErrorResponse(error=ErrorBody(code="unknownSymbol", message=f"Unknown symbol: {symbol}")),
    )


def invalid_combo(interval: str, range_key: str) -> ApiError:
    return ApiError(
        400,
        ErrorResponse(
            error=ErrorBody(
                code="invalidCombo",
                message=f"Invalid interval/range combination: {interval}/{range_key}",
            )
        ),
    )


def upstream_unavailable() -> ApiError:
    return ApiError(503, ErrorResponse(error=ErrorBody(code="upstreamUnavailable", message=_EOD_MSG)))
