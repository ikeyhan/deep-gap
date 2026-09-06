"""Payment provider abstraction (spec rules 17, 104).

Adding a gateway = implement PaymentProvider + register it. Verification is
ALWAYS server-side; a client-reported success is never trusted.

Two gateway shapes are supported by the same interface:
- Redirect gateways (e.g. Zarinpal): create_payment returns a redirect URL and
  an authority; verify is called on the callback.
- In-app purchase / market receipts (e.g. Bazaar, Myket): create_payment is a
  no-op and verify validates a purchase token server-side.
"""
from __future__ import annotations

import abc
from dataclasses import dataclass, field
from decimal import Decimal


@dataclass
class CreatePaymentResult:
    # For redirect gateways: where to send the user.
    payment_url: str | None
    # Gateway-side reference (authority / token). Stored on the Payment row.
    provider_ref: str
    extra: dict = field(default_factory=dict)


@dataclass
class VerifyResult:
    verified: bool
    provider_ref: str
    amount: Decimal | None = None
    detail: str | None = None


class PaymentProvider(abc.ABC):
    key: str = "base"

    @abc.abstractmethod
    async def create_payment(
        self,
        *,
        amount: Decimal,
        currency: str,
        callback_url: str,
        description: str,
        idempotency_key: str,
    ) -> CreatePaymentResult:
        ...

    @abc.abstractmethod
    async def verify(self, *, provider_ref: str, params: dict) -> VerifyResult:
        ...
