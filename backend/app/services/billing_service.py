"""Billing service — cost estimation, charging and usage ledger (spec rules 12-14).

Server is the single source of truth for price and credit cost (spec rule 103).
The client can never set price, credit cost or balance.
"""
from __future__ import annotations

import uuid
from decimal import ROUND_HALF_UP, Decimal

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai import AIModel, ModelPricing, UsageLog
from app.services.ai.base import Usage
from app.services.wallet_service import WalletService

_THOUSAND = Decimal(1000)


def _q(value: Decimal) -> Decimal:
    return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


class BillingService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db
        self.wallet = WalletService(db)

    # ---- pricing math ----
    def user_charge(self, pricing: ModelPricing, usage: Usage, *, units: int = 0) -> Decimal:
        """Credit charged to the user for a given usage."""
        charge = (
            (Decimal(usage.input_tokens) / _THOUSAND) * pricing.user_price_input
            + (Decimal(usage.output_tokens) / _THOUSAND) * pricing.user_price_output
            + Decimal(units) * pricing.user_price_unit
        )
        return _q(max(charge, pricing.minimum_charge))

    def provider_cost(self, pricing: ModelPricing, usage: Usage, *, units: int = 0) -> Decimal:
        cost = (
            (Decimal(usage.input_tokens) / _THOUSAND) * pricing.provider_cost_input
            + (Decimal(usage.output_tokens) / _THOUSAND) * pricing.provider_cost_output
            + Decimal(units) * pricing.provider_cost_unit
            + pricing.internal_cost
        )
        return cost

    def estimate_max_charge(self, model: AIModel, pricing: ModelPricing) -> Decimal:
        """Upper-bound estimate used for the pre-flight balance check."""
        max_out = Decimal(model.max_tokens or 2000)
        est = Usage(input_tokens=1000, output_tokens=int(max_out))
        return self.user_charge(pricing, est, units=1)

    # ---- ledger ----
    async def open_usage(
        self,
        *,
        user_id: uuid.UUID,
        model: AIModel,
        request_type: str,
        request_id: str,
    ) -> UsageLog:
        log = UsageLog(
            user_id=user_id,
            model_id=model.id,
            provider_id=model.provider_id,
            request_type=request_type,
            request_id=request_id,
            status="pending",
        )
        self.db.add(log)
        await self.db.flush()
        return log

    async def settle_usage(
        self,
        *,
        log: UsageLog,
        pricing: ModelPricing,
        usage: Usage,
        latency_ms: int,
        units: int = 0,
    ) -> Decimal:
        """Charge the user, record profit and complete the usage row."""
        charge = self.user_charge(pricing, usage, units=units)
        cost = self.provider_cost(pricing, usage, units=units)

        await self.wallet.debit(
            log.user_id,
            charge,
            type="debit",
            reason=f"ai:{log.request_type}",
            reference_type="usage_log",
            reference_id=str(log.id),
            idempotency_key=f"usage:{log.request_id}",
        )

        log.input_tokens = usage.input_tokens
        log.output_tokens = usage.output_tokens
        log.provider_cost = cost
        log.charged_credit = charge
        log.gross_profit = charge - cost
        log.latency_ms = latency_ms
        log.status = "completed"
        await self.db.flush()
        return charge

    async def fail_usage(self, *, log: UsageLog, reason: str) -> None:
        """Mark a request failed. No charge is applied (spec rules 74, 75)."""
        log.status = "failed"
        log.meta = {**(log.meta or {}), "error": reason}
        await self.db.flush()
