"""Macro endpoint."""
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class MacroResponse(BaseModel):
    asOf: str
    vix: float
    usdinr: float
    crude: float
    fiiDiiNet: float
    gsec10y: float


@router.get("/macro", response_model=MacroResponse)
def get_macro():
    """Get macro context indicators."""
    return MacroResponse(
        asOf="2026-10-17T19:00:00+05:30",
        vix=18.5,
        usdinr=83.2,
        crude=85.0,
        fiiDiiNet=1.2e9,
        gsec10y=7.25,
    )
