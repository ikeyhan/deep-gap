"""Payment provider registry."""
from __future__ import annotations

from app.services.payments.base import PaymentProvider
from app.services.payments.mock import MockPaymentProvider
from app.services.payments.zarinpal import build_from_env

_REGISTRY: dict[str, PaymentProvider] = {}


def register(provider: PaymentProvider) -> None:
    _REGISTRY[provider.key] = provider


def get_payment_provider(key: str) -> PaymentProvider | None:
    return _REGISTRY.get(key)


def bootstrap_payment_providers() -> None:
    register(MockPaymentProvider())
    zarinpal = build_from_env()
    if zarinpal:
        register(zarinpal)


bootstrap_payment_providers()
