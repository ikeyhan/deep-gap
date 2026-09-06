"""Payment provider + fulfillment unit tests (spec rules 17, 72, 104)."""
from datetime import UTC, datetime
from decimal import Decimal

import pytest

from app.services.payments.mock import MockPaymentProvider
from app.services.payments.registry import get_payment_provider
from app.services.subscription_service import compute_period


@pytest.mark.asyncio
async def test_mock_create_and_verify_roundtrip():
    provider = MockPaymentProvider()
    created = await provider.create_payment(
        amount=Decimal("99000"),
        currency="IRR",
        callback_url="http://cb.local/callback",
        description="test",
        idempotency_key="k1",
    )
    # Parse back the signed params the gateway would echo on the callback.
    from urllib.parse import parse_qs, urlparse

    q = parse_qs(urlparse(created.payment_url).query)
    params = {
        "status": q["status"][0],
        "signature": q["signature"][0],
        "amount": "99000",
    }
    result = await provider.verify(provider_ref=created.provider_ref, params=params)
    assert result.verified
    assert result.amount == Decimal("99000")


@pytest.mark.asyncio
async def test_mock_rejects_tampered_signature():
    provider = MockPaymentProvider()
    created = await provider.create_payment(
        amount=Decimal("50000"),
        currency="IRR",
        callback_url="http://cb.local/callback",
        description="test",
        idempotency_key="k2",
    )
    bad = {"status": "OK", "signature": "deadbeef" * 4, "amount": "50000"}
    result = await provider.verify(provider_ref=created.provider_ref, params=bad)
    assert not result.verified


@pytest.mark.asyncio
async def test_mock_rejects_amount_mismatch():
    provider = MockPaymentProvider()
    created = await provider.create_payment(
        amount=Decimal("50000"),
        currency="IRR",
        callback_url="http://cb.local/callback",
        description="test",
        idempotency_key="k3",
    )
    from urllib.parse import parse_qs, urlparse

    q = parse_qs(urlparse(created.payment_url).query)
    # Signature was for 50000 but claim a different amount → verify must fail.
    tampered = {"status": "OK", "signature": q["signature"][0], "amount": "999999"}
    result = await provider.verify(provider_ref=created.provider_ref, params=tampered)
    assert not result.verified


def test_registry_has_mock():
    assert get_payment_provider("mock") is not None


def test_compute_period():
    start = datetime(2026, 1, 1, tzinfo=UTC)
    s, e = compute_period(start, 30)
    assert s == start
    assert (e - s).days == 30
