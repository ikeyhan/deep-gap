"""Wallet and ledger models — the financial source of truth (spec rules 11, 73)."""
from __future__ import annotations

import uuid
from decimal import Decimal

from sqlalchemy import ForeignKey, Numeric, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.base import TimestampMixin, UUIDMixin


class Wallet(UUIDMixin, TimestampMixin, Base):
    __tablename__ = "wallets"

    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    # All amounts are in integer "credits" stored as Numeric for safety.
    balance: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    locked_balance: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    lifetime_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    spent_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)
    bonus_credit: Mapped[Decimal] = mapped_column(Numeric(18, 2), default=0, nullable=False)

    transactions: Mapped[list[WalletTransaction]] = relationship(back_populates="wallet")


class WalletTransaction(UUIDMixin, TimestampMixin, Base):
    """Immutable ledger entry. balance is NEVER mutated without a row here."""

    __tablename__ = "wallet_transactions"
    __table_args__ = (
        UniqueConstraint("idempotency_key", name="uq_wallet_tx_idempotency"),
    )

    wallet_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("wallets.id", ondelete="CASCADE"), index=True, nullable=False
    )
    # credit | debit | lock | unlock | refund | bonus | adjustment | purchase
    type: Mapped[str] = mapped_column(String(32), nullable=False)
    # Positive for credit, negative for debit.
    amount: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    balance_after: Mapped[Decimal] = mapped_column(Numeric(18, 2), nullable=False)
    reason: Mapped[str | None] = mapped_column(String(255), nullable=True)
    reference_type: Mapped[str | None] = mapped_column(String(64), nullable=True)
    reference_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    idempotency_key: Mapped[str | None] = mapped_column(String(128), nullable=True)
    meta: Mapped[dict | None] = mapped_column(JSONB, nullable=True)

    wallet: Mapped[Wallet] = relationship(back_populates="transactions")
