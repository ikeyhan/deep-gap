from __future__ import annotations

import uuid
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel


class AdminLogin(BaseModel):
    email: str
    password: str


class AdminToken(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str


class ModelPricingIn(BaseModel):
    provider_cost_input: Decimal = Decimal(0)
    provider_cost_output: Decimal = Decimal(0)
    provider_cost_unit: Decimal = Decimal(0)
    internal_cost: Decimal = Decimal(0)
    user_price_input: Decimal = Decimal(0)
    user_price_output: Decimal = Decimal(0)
    user_price_unit: Decimal = Decimal(0)
    minimum_charge: Decimal = Decimal(1)


class ModelCreate(BaseModel):
    provider_key: str
    provider_model: str
    code: str
    display_name: str
    description: str | None = None
    capability: str = "text"
    is_premium: bool = False
    min_tier: str = "free"
    display_order: int = 100
    max_tokens: int | None = 4096
    fallback_codes: list[str] | None = None
    pricing: ModelPricingIn


class ModelUpdate(BaseModel):
    display_name: str | None = None
    description: str | None = None
    is_enabled: bool | None = None
    is_premium: bool | None = None
    min_tier: str | None = None
    display_order: int | None = None
    max_tokens: int | None = None
    fallback_codes: list[str] | None = None
    region_rules: dict | None = None
    pricing: ModelPricingIn | None = None


class AdminModelOut(BaseModel):
    id: uuid.UUID
    code: str
    display_name: str
    capability: str
    is_enabled: bool
    is_premium: bool
    min_tier: str
    display_order: int
    provider_model: str

    class Config:
        from_attributes = True


class AdminUserOut(BaseModel):
    id: uuid.UUID
    phone: str
    is_active: bool
    is_blocked: bool
    created_at: datetime

    class Config:
        from_attributes = True


class CreditAdjustIn(BaseModel):
    amount: Decimal  # positive = add, negative = deduct
    reason: str
