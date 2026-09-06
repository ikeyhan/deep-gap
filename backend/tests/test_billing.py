"""Unit tests for billing math — no DB required (spec rule 49: billing tests)."""
from decimal import Decimal

from app.models.ai import AIModel, ModelPricing
from app.services.ai.base import Usage
from app.services.billing_service import BillingService


def _pricing(**kw) -> ModelPricing:
    defaults = dict(
        provider_cost_input=Decimal("0.10"),
        provider_cost_output=Decimal("0.20"),
        provider_cost_unit=Decimal("0"),
        internal_cost=Decimal("0.01"),
        user_price_input=Decimal("0.25"),
        user_price_output=Decimal("0.50"),
        user_price_unit=Decimal("0"),
        minimum_charge=Decimal("1"),
        minimum_profit=Decimal("0"),
    )
    defaults.update(kw)
    return ModelPricing(**defaults)


def test_user_charge_tokens():
    billing = BillingService(db=None)  # type: ignore[arg-type]
    pricing = _pricing()
    usage = Usage(input_tokens=1000, output_tokens=2000)
    # 1*0.25 + 2*0.50 = 1.25
    assert billing.user_charge(pricing, usage) == Decimal("1.25")


def test_minimum_charge_applies():
    billing = BillingService(db=None)  # type: ignore[arg-type]
    pricing = _pricing(minimum_charge=Decimal("5"))
    usage = Usage(input_tokens=10, output_tokens=10)
    assert billing.user_charge(pricing, usage) == Decimal("5.00")


def test_gross_profit_positive():
    billing = BillingService(db=None)  # type: ignore[arg-type]
    pricing = _pricing()
    usage = Usage(input_tokens=1000, output_tokens=1000)
    charge = billing.user_charge(pricing, usage)
    cost = billing.provider_cost(pricing, usage)
    assert charge - cost > 0  # margin must never be negative (spec rule 58)


def test_estimate_max_charge_uses_max_tokens():
    billing = BillingService(db=None)  # type: ignore[arg-type]
    pricing = _pricing()
    model = AIModel(max_tokens=2000)
    est = billing.estimate_max_charge(model, pricing)
    assert est > Decimal("1")
