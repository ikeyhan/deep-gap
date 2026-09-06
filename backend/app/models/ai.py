"""AI provider, model, pricing and usage-ledger models.

Models are NOT hard-coded — the admin manages them from the DB (spec rule 10).
"""
from __future__ import annotations

import uuid
from decimal import Decimal

from sqlalchemy import Boolean, ForeignKey, Integer, Numeric, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.base import SoftDeleteMixin, TimestampMixin, UUIDMixin


class AIProviderRow(UUIDMixin, TimestampMixin, SoftDeleteMixin, Base):
    __tablename__ = "ai_providers"

    # Matches an adapter registered in app.services.ai.registry (e.g. "echo", "openai").
    key: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    is_enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    priority: Mapped[int] = mapped_column(Integer, default=100, nullable=False)
    config: Mapped[dict | None] = mapped_column(JSONB, nullable=True)

    models: Mapped[list[AIModel]] = relationship(back_populates="provider")


class AIModel(UUIDMixin, TimestampMixin, SoftDeleteMixin, Base):
    __tablename__ = "ai_models"

    provider_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("ai_providers.id"), index=True, nullable=False
    )
    # Provider-side model identifier, e.g. "gpt-4o-mini".
    provider_model: Mapped[str] = mapped_column(String(120), nullable=False)
    # Public code used by the app, e.g. "fast", "smart", "researcher".
    code: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    display_name: Mapped[str] = mapped_column(String(120), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    # text | image | vision | speech_to_text | text_to_speech | embedding | video | music
    capability: Mapped[str] = mapped_column(String(32), default="text", nullable=False)
    is_enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    is_premium: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    # Minimum subscription tier required: free | plus | pro
    min_tier: Mapped[str] = mapped_column(String(16), default="free", nullable=False)
    display_order: Mapped[int] = mapped_column(Integer, default=100, nullable=False)
    # Ordered list of model codes to fall back to when this one fails (spec rule 9).
    fallback_codes: Mapped[list | None] = mapped_column(JSONB, nullable=True)
    # Region visibility control (spec rule 10): {"block": ["XX"]} etc.
    region_rules: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    max_tokens: Mapped[int | None] = mapped_column(Integer, nullable=True)

    provider: Mapped[AIProviderRow] = relationship(back_populates="models")
    pricing: Mapped[ModelPricing] = relationship(
        back_populates="model", uselist=False, cascade="all, delete-orphan"
    )


class ModelPricing(UUIDMixin, TimestampMixin, Base):
    """Real-cost-based pricing (spec rule 12). All values in credits."""

    __tablename__ = "model_pricing"

    model_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("ai_models.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    # Cost we pay the provider, expressed per 1K input/output tokens (or per unit).
    provider_cost_input: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    provider_cost_output: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    # Flat cost per request for non-token capabilities (image/audio).
    provider_cost_unit: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    # Internal overhead added on top of provider cost.
    internal_cost: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    # What the user is charged.
    user_price_input: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    user_price_output: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    user_price_unit: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    minimum_charge: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=1, nullable=False)
    minimum_profit: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)

    model: Mapped[AIModel] = relationship(back_populates="pricing")


class UsageLog(UUIDMixin, TimestampMixin, Base):
    """One row per AI request — the usage ledger (spec rule 14)."""

    __tablename__ = "usage_logs"
    __table_args__ = (UniqueConstraint("request_id", name="uq_usage_request_id"),)

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), index=True, nullable=False
    )
    model_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("ai_models.id"), nullable=True
    )
    provider_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("ai_providers.id"), nullable=True
    )
    request_type: Mapped[str] = mapped_column(String(32), nullable=False)
    request_id: Mapped[str] = mapped_column(String(64), index=True, nullable=False)
    input_tokens: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    output_tokens: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    provider_cost: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    charged_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    gross_profit: Mapped[Decimal] = mapped_column(Numeric(18, 6), default=0, nullable=False)
    latency_ms: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    # pending | completed | failed | refunded
    status: Mapped[str] = mapped_column(String(16), default="pending", nullable=False)
    meta: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
