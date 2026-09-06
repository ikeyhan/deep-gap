"""Subscription & credit-package fulfillment.

Called only after a payment is verified server-side. All credit grants go
through the wallet ledger with a payment-scoped idempotency key so a repeated
callback can never double-credit.
"""
from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError
from app.models.subscription import CreditPackage, Subscription, SubscriptionPlan
from app.services.wallet_service import WalletService


def compute_period(start: datetime, duration_days: int) -> tuple[datetime, datetime]:
    return start, start + timedelta(days=duration_days)


class SubscriptionService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db
        self.wallet = WalletService(db)

    async def get_plan(self, plan_id: uuid.UUID) -> SubscriptionPlan:
        plan = await self.db.get(SubscriptionPlan, plan_id)
        if plan is None or not plan.is_active:
            raise NotFoundError("پلن یافت نشد", error_code="plan_not_found")
        return plan

    async def active_subscription(self, user_id: uuid.UUID) -> Subscription | None:
        stmt = (
            select(Subscription)
            .where(
                Subscription.user_id == user_id,
                Subscription.status == "active",
                Subscription.expires_at > datetime.now(UTC),
            )
            .order_by(Subscription.expires_at.desc())
            .limit(1)
        )
        return (await self.db.execute(stmt)).scalar_one_or_none()

    async def activate_from_payment(
        self, *, user_id: uuid.UUID, plan_id: uuid.UUID, payment_id: uuid.UUID
    ) -> Subscription:
        """Activate/extend a plan and grant its monthly credit (idempotent)."""
        plan = await self.get_plan(plan_id)
        now = datetime.now(UTC)

        current = await self.active_subscription(user_id)
        start = current.expires_at if current else now  # stack on top if still active
        started_at, expires_at = compute_period(start, plan.duration_days)

        sub = Subscription(
            user_id=user_id,
            plan_id=plan.id,
            status="active",
            started_at=started_at,
            expires_at=expires_at,
        )
        self.db.add(sub)

        if plan.monthly_credit > 0:
            await self.wallet.credit(
                user_id,
                Decimal(plan.monthly_credit),
                type="credit",
                reason=f"subscription:{plan.code}",
                reference_type="payment",
                reference_id=str(payment_id),
                idempotency_key=f"payment:{payment_id}:sub",
            )
        await self.db.flush()
        return sub

    async def grant_credit_package(
        self, *, user_id: uuid.UUID, package_id: uuid.UUID, payment_id: uuid.UUID
    ) -> Decimal:
        pkg = await self.db.get(CreditPackage, package_id)
        if pkg is None or not pkg.is_active:
            raise NotFoundError("بستهٔ اعتبار یافت نشد", error_code="package_not_found")

        total = Decimal(pkg.credit_amount)
        await self.wallet.credit(
            user_id,
            total,
            type="purchase",
            reason=f"credit_pack:{pkg.code}",
            reference_type="payment",
            reference_id=str(payment_id),
            idempotency_key=f"payment:{payment_id}:pack",
        )
        if pkg.bonus_credit > 0:
            await self.wallet.credit(
                user_id,
                Decimal(pkg.bonus_credit),
                type="bonus",
                reason=f"credit_pack_bonus:{pkg.code}",
                reference_type="payment",
                reference_id=str(payment_id),
                idempotency_key=f"payment:{payment_id}:pack_bonus",
                is_bonus=True,
            )
        await self.db.flush()
        return total + Decimal(pkg.bonus_credit)
