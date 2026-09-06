"""Platform/ops models: admin, audit, config, payments, referrals, OTP records."""
from __future__ import annotations

import uuid
from datetime import datetime
from decimal import Decimal

from sqlalchemy import Boolean, DateTime, ForeignKey, Numeric, String, Text
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.models.base import TimestampMixin, UUIDMixin


class AdminUser(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "admin_users"

    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    # super_admin | support | finance | content | analyst  (spec rule 78)
    role: Mapped[str] = mapped_column(String(32), default="support", nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    totp_secret: Mapped[str | None] = mapped_column(String(64), nullable=True)  # 2FA-ready


class AuditLog(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "audit_logs"

    actor_type: Mapped[str] = mapped_column(String(16), default="admin", nullable=False)
    actor_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True)
    action: Mapped[str] = mapped_column(String(64), nullable=False)
    target_type: Mapped[str | None] = mapped_column(String(64), nullable=True)
    target_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    meta: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    ip_address: Mapped[str | None] = mapped_column(String(64), nullable=True)


class FeatureFlag(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "feature_flags"

    key: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    enabled: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # Optional gradual rollout percentage / audience rules.
    rules: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)


class RemoteConfig(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "remote_config"

    key: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    value: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)


class Payment(UUIDMixin, TimestampMixin, Base):
    """A payment attempt. Verified server-side only (spec rules 17, 104)."""

    __tablename__ = "payments"

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), index=True, nullable=False
    )
    provider: Mapped[str] = mapped_column(String(32), nullable=False)  # zarinpal | bazaar | ...
    # subscription | credit_package
    product_type: Mapped[str] = mapped_column(String(32), nullable=False)
    product_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    amount: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    currency: Mapped[str] = mapped_column(String(8), default="IRR", nullable=False)
    # initiated | pending | verified | failed | refunded
    status: Mapped[str] = mapped_column(String(16), default="initiated", nullable=False)
    provider_ref: Mapped[str | None] = mapped_column(String(255), index=True, nullable=True)
    idempotency_key: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    meta: Mapped[dict | None] = mapped_column(JSONB, nullable=True)


class Referral(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "referrals"

    referrer_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), index=True, nullable=False
    )
    referred_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), unique=True, nullable=False
    )
    reward_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    status: Mapped[str] = mapped_column(String(16), default="pending", nullable=False)
