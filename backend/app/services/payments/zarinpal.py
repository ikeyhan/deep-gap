"""Zarinpal gateway adapter (real-shaped example of a redirect gateway).

Registered only when ZARINPAL_MERCHANT_ID is configured. Amounts are in IRR.
Server-side verify is authoritative.
"""
from __future__ import annotations

import os
from decimal import Decimal

import httpx

from app.core.logging import get_logger
from app.services.payments.base import (
    CreatePaymentResult,
    PaymentProvider,
    VerifyResult,
)

log = get_logger("zarinpal")

_REQUEST_URL = "https://payment.zarinpal.com/pg/v4/payment/request.json"
_VERIFY_URL = "https://payment.zarinpal.com/pg/v4/payment/verify.json"
_STARTPAY = "https://payment.zarinpal.com/pg/StartPay/"


class ZarinpalProvider(PaymentProvider):
    key = "zarinpal"

    def __init__(self, merchant_id: str) -> None:
        self.merchant_id = merchant_id

    async def create_payment(
        self,
        *,
        amount: Decimal,
        currency: str,
        callback_url: str,
        description: str,
        idempotency_key: str,
    ) -> CreatePaymentResult:
        payload = {
            "merchant_id": self.merchant_id,
            "amount": int(amount),
            "callback_url": callback_url,
            "description": description,
        }
        async with httpx.AsyncClient(timeout=20) as client:
            res = await client.post(_REQUEST_URL, json=payload)
            data = res.json().get("data", {})
        authority = data.get("authority")
        if not authority:
            raise RuntimeError(f"zarinpal request failed: {res.text}")
        return CreatePaymentResult(
            payment_url=f"{_STARTPAY}{authority}",
            provider_ref=authority,
            extra={"amount": str(amount)},
        )

    async def verify(self, *, provider_ref: str, params: dict) -> VerifyResult:
        # Zarinpal returns Status=OK and Authority on the callback; verify with amount.
        if params.get("Status") != "OK" and params.get("status") != "OK":
            return VerifyResult(False, provider_ref, detail="cancelled")
        amount = Decimal(str(params["amount"]))
        payload = {
            "merchant_id": self.merchant_id,
            "amount": int(amount),
            "authority": provider_ref,
        }
        async with httpx.AsyncClient(timeout=20) as client:
            res = await client.post(_VERIFY_URL, json=payload)
            data = res.json().get("data", {})
        code = data.get("code")
        # 100 = verified, 101 = already verified (idempotent success).
        if code in (100, 101):
            return VerifyResult(True, provider_ref, amount=amount, detail=str(code))
        return VerifyResult(False, provider_ref, detail=f"code={code}")


def build_from_env() -> ZarinpalProvider | None:
    merchant = os.getenv("ZARINPAL_MERCHANT_ID", "")
    if not merchant:
        return None
    return ZarinpalProvider(merchant)
