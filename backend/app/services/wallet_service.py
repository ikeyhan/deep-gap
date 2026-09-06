"""Wallet service — the ONLY place wallet balances change (spec rules 73, 105).

Every balance change writes an immutable ledger row inside the same DB
transaction. A row-level lock (SELECT ... FOR UPDATE) prevents race conditions,
and an optional idempotency key makes retries safe.
"""
from __future__ import annotations

import uuid
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import InsufficientCreditError
from app.models.wallet import Wallet, WalletTransaction


class WalletService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db

    async def get_or_create(self, user_id: uuid.UUID) -> Wallet:
        wallet = (
            await self.db.execute(select(Wallet).where(Wallet.user_id == user_id))
        ).scalar_one_or_none()
        if wallet is None:
            wallet = Wallet(user_id=user_id)
            self.db.add(wallet)
            await self.db.flush()
        return wallet

    async def _locked_wallet(self, user_id: uuid.UUID) -> Wallet:
        wallet = (
            await self.db.execute(
                select(Wallet).where(Wallet.user_id == user_id).with_for_update()
            )
        ).scalar_one_or_none()
        if wallet is None:
            wallet = await self.get_or_create(user_id)
            # Re-select with lock now that it exists.
            wallet = (
                await self.db.execute(
                    select(Wallet).where(Wallet.id == wallet.id).with_for_update()
                )
            ).scalar_one()
        return wallet

    async def _find_by_idempotency(self, key: str | None) -> WalletTransaction | None:
        if not key:
            return None
        return (
            await self.db.execute(
                select(WalletTransaction).where(WalletTransaction.idempotency_key == key)
            )
        ).scalar_one_or_none()

    async def credit(
        self,
        user_id: uuid.UUID,
        amount: Decimal,
        *,
        type: str = "credit",
        reason: str | None = None,
        reference_type: str | None = None,
        reference_id: str | None = None,
        idempotency_key: str | None = None,
        is_bonus: bool = False,
    ) -> WalletTransaction:
        assert amount > 0, "credit amount must be positive"
        existing = await self._find_by_idempotency(idempotency_key)
        if existing:
            return existing

        wallet = await self._locked_wallet(user_id)
        wallet.balance += amount
        wallet.lifetime_credit += amount
        if is_bonus:
            wallet.bonus_credit += amount

        tx = WalletTransaction(
            wallet_id=wallet.id,
            type=type,
            amount=amount,
            balance_after=wallet.balance,
            reason=reason,
            reference_type=reference_type,
            reference_id=reference_id,
            idempotency_key=idempotency_key,
        )
        self.db.add(tx)
        await self.db.flush()
        return tx

    async def debit(
        self,
        user_id: uuid.UUID,
        amount: Decimal,
        *,
        type: str = "debit",
        reason: str | None = None,
        reference_type: str | None = None,
        reference_id: str | None = None,
        idempotency_key: str | None = None,
    ) -> WalletTransaction:
        assert amount > 0, "debit amount must be positive"
        existing = await self._find_by_idempotency(idempotency_key)
        if existing:
            return existing

        wallet = await self._locked_wallet(user_id)
        if wallet.balance < amount:
            raise InsufficientCreditError("اعتبار کافی نیست")

        wallet.balance -= amount
        wallet.spent_credit += amount

        tx = WalletTransaction(
            wallet_id=wallet.id,
            type=type,
            amount=-amount,
            balance_after=wallet.balance,
            reason=reason,
            reference_type=reference_type,
            reference_id=reference_id,
            idempotency_key=idempotency_key,
        )
        self.db.add(tx)
        await self.db.flush()
        return tx

    async def refund(
        self,
        user_id: uuid.UUID,
        amount: Decimal,
        *,
        reason: str | None = None,
        reference_type: str | None = None,
        reference_id: str | None = None,
        idempotency_key: str | None = None,
    ) -> WalletTransaction:
        """Return credit to the user (spec rule 74). Reduces spent_credit."""
        tx = await self.credit(
            user_id,
            amount,
            type="refund",
            reason=reason,
            reference_type=reference_type,
            reference_id=reference_id,
            idempotency_key=idempotency_key,
        )
        wallet = await self._locked_wallet(user_id)
        wallet.spent_credit = max(Decimal(0), wallet.spent_credit - amount)
        await self.db.flush()
        return tx
