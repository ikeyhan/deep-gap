from __future__ import annotations

import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel


class PlanOut(BaseModel):
    id: uuid.UUID
    code: str
    name: str
    price: Decimal
    duration_days: int
    monthly_credit: Decimal
    features: dict | None
    display_order: int

    class Config:
        from_attributes = True


class CreditPackageOut(BaseModel):
    id: uuid.UUID
    code: str
    name: str
    price: Decimal
    credit_amount: Decimal
    bonus_credit: Decimal
    display_order: int

    class Config:
        from_attributes = True


class SubscriptionOut(BaseModel):
    id: uuid.UUID
    plan_id: uuid.UUID
    status: str
    started_at: datetime
    expires_at: datetime
    auto_renew: bool

    class Config:
        from_attributes = True


class PaymentInitiateRequest(BaseModel):
    # "subscription" | "credit_package"
    product_type: str
    code: str


class PaymentInitiateResponse(BaseModel):
    payment_id: uuid.UUID
    status: str
    amount: Decimal
    payment_url: str | None


class PaymentStatusOut(BaseModel):
    id: uuid.UUID
    status: str
    product_type: str
    amount: Decimal
    verified_at: datetime | None

    class Config:
        from_attributes = True
