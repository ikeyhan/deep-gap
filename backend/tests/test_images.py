"""Image generation billing + echo image provider tests (no DB)."""
from decimal import Decimal

import pytest

from app.models.ai import ModelPricing
from app.services.ai.base import Usage
from app.services.ai.providers.echo import EchoProvider
from app.services.billing_service import BillingService


def _image_pricing() -> ModelPricing:
    return ModelPricing(
        provider_cost_input=Decimal(0),
        provider_cost_output=Decimal(0),
        provider_cost_unit=Decimal("2000"),
        internal_cost=Decimal("0.01"),
        user_price_input=Decimal(0),
        user_price_output=Decimal(0),
        user_price_unit=Decimal("5000"),
        minimum_charge=Decimal(1),
        minimum_profit=Decimal(0),
    )


def test_unit_charge_scales_with_images():
    billing = BillingService(db=None)  # type: ignore[arg-type]
    pricing = _image_pricing()
    # 1 image → 5000 credits; margin positive.
    charge1 = billing.user_charge(pricing, Usage(), units=1)
    assert charge1 == Decimal("5000.00")
    cost1 = billing.provider_cost(pricing, Usage(), units=1)
    assert charge1 - cost1 > 0
    # 3 images → 3x.
    charge3 = billing.user_charge(pricing, Usage(), units=3)
    assert charge3 == Decimal("15000.00")


@pytest.mark.asyncio
async def test_echo_generates_images():
    provider = EchoProvider()
    from app.services.ai.base import Capability, ImageRequest

    assert provider.supports(Capability.IMAGE)
    res = await provider.generate_image(
        ImageRequest(model="echo-designer", prompt="یک گربه", size="512x512", n=2)
    )
    assert len(res.urls) == 2
