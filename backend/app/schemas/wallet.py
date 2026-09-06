from __future__ import annotations

import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel


class WalletOut(BaseModel):
    balance: Decimal
    locked_balance: Decimal
    lifetime_credit: Decimal
    spent_credit: Decimal
    bonus_credit: Decimal

    class Config:
        from_attributes = True


class TransactionOut(BaseModel):
    id: uuid.UUID
    type: str
    amount: Decimal
    balance_after: Decimal
    reason: str | None
    created_at: datetime

    class Config:
        from_attributes = True


class ModelOut(BaseModel):
    code: str
    display_name: str
    description: str | None
    capability: str
    is_premium: bool
    min_tier: str
    display_order: int

    class Config:
        from_attributes = True
