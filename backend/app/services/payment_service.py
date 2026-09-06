"""Payment orchestration: initiate + server-side verify + fulfillment.

Rules enforced here:
- Price comes from the DB, never from the client (spec rule 103).
- A purchase is real only after server-side verification (spec rules 17, 104).
- Fulfillment (credit / subscription) is idempotent, so a repeated callback
  never double-grants (spec rule 72).
- Every money movement is audited via the wallet ledger (spec rule 105).
"""
from __future__ import annotations

import uuid
from datetime import UTC, datetime
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import AppError, NotFoundError, ValidationError
from app.core.logging import get_logger
from app.models.platform import Payment
from app.models.subscription import CreditPackage, SubscriptionPlan
from app.services.payments.registry import get_payment_provider
from app.services.subscription_service import SubscriptionService

log = get_logger("payment")


class PaymentService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db

    async def _resolve_product(
        self, product_type: str, code: str
    ) -> tuple[uuid.UUID, Decimal, str]:
        """Return (product_id, amount, description). Price is authoritative."""
        if product_type == "subscription":
            plan = (
                await self.db.execute(
                    select(SubscriptionPlan).where(
                        SubscriptionPlan.code == code, SubscriptionPlan.is_active.is_(True)
                    )
                )
            ).scalar_one_or_none()
            if plan is None:
                raise NotFoundError("پلن یافت نشد", error_code="plan_not_found")
            return plan.id, Decimal(plan.price), f"اشتراک {plan.name}"

        if product_type == "credit_package":
            pkg = (
                await self.db.execute(
                    select(CreditPackage).where(
                        CreditPackage.code == code, CreditPackage.is_active.is_(True)
                    )
                )
            ).scalar_one_or_none()
            if pkg is None:
                raise NotFoundError("بستهٔ اعتبار یافت نشد", error_code="package_not_found")
            return pkg.id, Decimal(pkg.price), f"بستهٔ اعتبار {pkg.name}"

        raise ValidationError("نوع محصول نامعتبر است", error_code="invalid_product_type")

    async def initiate(
        self, *, user_id: uuid.UUID, product_type: str, code: str
    ) -> tuple[Payment, str | None]:
        product_id, amount, description = await self._resolve_product(product_type, code)
        if amount <= 0:
            raise ValidationError("این محصول رایگان است و نیازی به پرداخت ندارد")

        provider_key = settings.payment_provider
        provider = get_payment_provider(provider_key)
        if provider is None:
            raise AppError(
                "درگاه پرداخت در دسترس نیست",
                error_code="gateway_unavailable",
                status_code=503,
            )

        idempotency_key = f"pay:{user_id}:{product_type}:{code}:{uuid.uuid4().hex[:12]}"
        payment = Payment(
            user_id=user_id,
            provider=provider_key,
            product_type=product_type,
            product_id=str(product_id),
            amount=amount,
            currency="IRR",
            status="initiated",
            idempotency_key=idempotency_key,
        )
        self.db.add(payment)
        await self.db.flush()

        result = await provider.create_payment(
            amount=amount,
            currency="IRR",
            callback_url=f"{settings.payment_callback_url}?payment_id={payment.id}",
            description=description,
            idempotency_key=idempotency_key,
        )
        payment.provider_ref = result.provider_ref
        payment.status = "pending"
        payment.meta = {**(payment.meta or {}), **result.extra}
        await self.db.flush()
        return payment, result.payment_url

    async def verify(self, *, payment_id: uuid.UUID, params: dict) -> Payment:
        payment = await self.db.get(Payment, payment_id)
        if payment is None:
            raise NotFoundError("پرداخت یافت نشد", error_code="payment_not_found")

        # Idempotent: an already-verified payment just returns (fulfillment done).
        if payment.status == "verified":
            return payment
        if payment.status == "refunded":
            raise ValidationError("این پرداخت بازگردانده شده است")

        provider = get_payment_provider(payment.provider)
        if provider is None:
            raise AppError(
                "درگاه پرداخت در دسترس نیست",
                error_code="gateway_unavailable",
                status_code=503,
            )

        # Carry the authoritative amount into verification.
        verify_params = {**params, "amount": str(payment.amount)}
        result = await provider.verify(
            provider_ref=payment.provider_ref or "", params=verify_params
        )

        if not result.verified:
            payment.status = "failed"
            payment.meta = {**(payment.meta or {}), "verify_detail": result.detail}
            await self.db.flush()
            raise ValidationError("تأیید پرداخت ناموفق بود", error_code="verification_failed")

        # Amount tampering guard.
        if result.amount is not None and Decimal(result.amount) != Decimal(payment.amount):
            payment.status = "failed"
            payment.meta = {**(payment.meta or {}), "verify_detail": "amount_mismatch"}
            await self.db.flush()
            raise ValidationError("مبلغ پرداخت مطابقت ندارد", error_code="amount_mismatch")

        payment.status = "verified"
        payment.verified_at = datetime.now(UTC)
        await self.db.flush()

        # Fulfillment (idempotent by payment id).
        subs = SubscriptionService(self.db)
        product_uuid = uuid.UUID(payment.product_id)
        if payment.product_type == "subscription":
            await subs.activate_from_payment(
                user_id=payment.user_id, plan_id=product_uuid, payment_id=payment.id
            )
        elif payment.product_type == "credit_package":
            await subs.grant_credit_package(
                user_id=payment.user_id, package_id=product_uuid, payment_id=payment.id
            )
        await self.db.flush()
        return payment
