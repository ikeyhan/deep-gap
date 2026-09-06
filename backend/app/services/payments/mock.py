"""Mock payment provider for development and tests.

create_payment returns a fake gateway URL carrying a signed authority. verify
recomputes the signature server-side, so only a correctly-signed callback (which
in real life only the gateway could produce) is accepted. This mirrors how a
real gateway proves the payment without trusting the client.
"""
from __future__ import annotations

import hashlib
import hmac
import uuid
from decimal import Decimal

from app.core.config import settings
from app.services.payments.base import (
    CreatePaymentResult,
    PaymentProvider,
    VerifyResult,
)


def _sign(authority: str, amount: Decimal) -> str:
    msg = f"{authority}:{amount}".encode()
    return hmac.new(settings.secret_key.encode(), msg, hashlib.sha256).hexdigest()[:32]


class MockPaymentProvider(PaymentProvider):
    key = "mock"

    async def create_payment(
        self,
        *,
        amount: Decimal,
        currency: str,
        callback_url: str,
        description: str,
        idempotency_key: str,
    ) -> CreatePaymentResult:
        authority = "MOCK-" + uuid.uuid4().hex[:20]
        signature = _sign(authority, amount)
        url = (
            f"{callback_url}?provider_ref={authority}"
            f"&status=OK&signature={signature}"
        )
        return CreatePaymentResult(
            payment_url=url, provider_ref=authority, extra={"amount": str(amount)}
        )

    async def verify(self, *, provider_ref: str, params: dict) -> VerifyResult:
        status = params.get("status")
        signature = params.get("signature", "")
        amount_raw = params.get("amount")
        amount = Decimal(str(amount_raw)) if amount_raw is not None else None

        if status != "OK":
            return VerifyResult(False, provider_ref, detail="payment_not_ok")
        if amount is None:
            return VerifyResult(False, provider_ref, detail="amount_missing")

        expected = _sign(provider_ref, amount)
        if not hmac.compare_digest(expected, signature):
            return VerifyResult(False, provider_ref, detail="bad_signature")
        return VerifyResult(True, provider_ref, amount=amount)
