"""Tradeplan endpoint."""
from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List, Optional
from enum import Enum

router = APIRouter()


class Pattern(str, Enum):
    head_and_shoulders = "head_and_shoulders"
    cup_and_handle = "cup_and_handle"
    double_triple = "double_triple"
    flag = "flag"


class PricePoint(BaseModel):
    t: str
    price: float


class ActivePattern(BaseModel):
    pattern: str
    entry: PricePoint
    stop: PricePoint
    target: PricePoint
    rr: float
    direction: str
    invalidation: str
    positionSize: int


class TradeplanResponse(BaseModel):
    active: List[ActivePattern]


@router.get("/tradeplan", response_model=TradeplanResponse)
def get_tradeplan(
    symbol: str = Query(..., description="Stock symbol"),
    pattern: Optional[Pattern] = Query(None, description="Pattern"),
):
    """Get trade plan for an active pattern."""
    return TradeplanResponse(active=[])
