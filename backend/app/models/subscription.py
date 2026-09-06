"""Subscription plans, user subscriptions and credit packages."""
from __future__ import annotations

import uuid
from datetime import datetime
from decimal import Decimal

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, Numeric, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.base import TimestampMixin, UUIDMixin


class SubscriptionPlan(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "subscription_plans"

    code: Mapped[str] = mapped_column(String(32), unique=True, nullable=False)  # free|plus|pro
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    price: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    duration_days: Mapped[int] = mapped_column(Integer, default=30, nullable=False)
    monthly_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    # Feature/limit matrix, e.g. {"daily_messages": 20, "web_search": false}
    features: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    display_order: Mapped[int] = mapped_column(Integer, default=100, nullable=False)


class Subscription(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "subscriptions"

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), index=True, nullable=False
    )
    plan_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("subscription_plans.id"), nullable=False
    )
    status: Mapped[str] = mapped_column(String(16), default="active", nullable=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    auto_renew: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    cancelled_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class CreditPackage(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "credit_packages"

    code: Mapped[str] = mapped_column(String(32), unique=True, nullable=False)
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    price: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)  # money price
    credit_amount: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    bonus_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    display_order: Mapped[int] = mapped_column(Integer, default=100, nullable=False)
