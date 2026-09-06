"""Seed default data: providers, models, pricing, plans, credit packs, config.

Run with:  python -m app.seed
Idempotent — safe to run multiple times.
"""
from __future__ import annotations

import asyncio
from decimal import Decimal

from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.security import hash_password
from app.models.ai import AIModel, AIProviderRow, ModelPricing
from app.models.platform import AdminUser, FeatureFlag, RemoteConfig
from app.models.subscription import CreditPackage, SubscriptionPlan

# Persona-based catalogue (spec rule 2) — the app shows personas, not raw model names.
MODELS = [
    dict(code="fast", name="سریع", capability="text", tier="free", order=1,
         desc="مناسب سؤال روزمره، ترجمه و خلاصه‌سازی", premium=False,
         u_in=Decimal("0.25"), u_out=Decimal("0.50"), c_in=Decimal("0.10"), c_out=Decimal("0.20")),
    dict(code="smart", name="هوشمند", capability="text", tier="plus", order=2,
         desc="مناسب تحلیل، برنامه‌نویسی و نوشتن حرفه‌ای", premium=True,
         u_in=Decimal("1.30"), u_out=Decimal("2.60"), c_in=Decimal("0.50"), c_out=Decimal("1.00")),
    dict(code="researcher", name="پژوهشگر", capability="text", tier="pro", order=3,
         desc="مناسب تحقیق عمیق و گزارش حرفه‌ای", premium=True,
         u_in=Decimal("3.00"), u_out=Decimal("6.00"), c_in=Decimal("1.20"), c_out=Decimal("2.40")),
    dict(code="designer", name="طراح", capability="image", tier="plus", order=4,
         desc="تولید و ویرایش تصویر", premium=True,
         u_unit=Decimal("5000"), c_unit=Decimal("2000")),
]


async def _seed_providers(db):
    row = (await db.execute(select(AIProviderRow).where(AIProviderRow.key == "echo"))).scalar_one_or_none()
    if row is None:
        row = AIProviderRow(key="echo", name="Echo (Mock Provider)", is_enabled=True, priority=100)
        db.add(row)
        await db.flush()
    return row


async def _seed_models(db, provider):
    for m in MODELS:
        existing = (
            await db.execute(select(AIModel).where(AIModel.code == m["code"]))
        ).scalar_one_or_none()
        if existing:
            continue
        model = AIModel(
            provider_id=provider.id,
            provider_model="echo-" + m["code"],
            code=m["code"],
            display_name=m["name"],
            description=m["desc"],
            capability=m["capability"],
            is_premium=m["premium"],
            min_tier=m["tier"],
            display_order=m["order"],
            max_tokens=4096,
        )
        db.add(model)
        await db.flush()
        db.add(
            ModelPricing(
                model_id=model.id,
                provider_cost_input=m.get("c_in", Decimal(0)),
                provider_cost_output=m.get("c_out", Decimal(0)),
                provider_cost_unit=m.get("c_unit", Decimal(0)),
                internal_cost=Decimal("0.01"),
                user_price_input=m.get("u_in", Decimal(0)),
                user_price_output=m.get("u_out", Decimal(0)),
                user_price_unit=m.get("u_unit", Decimal(0)),
                minimum_charge=Decimal("1"),
            )
        )


async def _seed_plans(db):
    plans = [
        dict(code="free", name="رایگان", price=Decimal(0), credit=Decimal(100), order=1,
             features={"daily_messages": 20, "web_search": False, "voice": False}),
        dict(code="plus", name="پلاس", price=Decimal(99000), credit=Decimal(2000), order=2,
             features={"daily_messages": 200, "web_search": False, "voice": False, "files": True}),
        dict(code="pro", name="پرو", price=Decimal(249000), credit=Decimal(6000), order=3,
             features={"daily_messages": 1000, "web_search": True, "voice": True, "memory": True}),
    ]
    for p in plans:
        existing = (
            await db.execute(select(SubscriptionPlan).where(SubscriptionPlan.code == p["code"]))
        ).scalar_one_or_none()
        if existing:
            continue
        db.add(
            SubscriptionPlan(
                code=p["code"], name=p["name"], price=p["price"],
                monthly_credit=p["credit"], features=p["features"], display_order=p["order"],
            )
        )


async def _seed_credit_packs(db):
    packs = [
        dict(code="starter", name="بستهٔ شروع", price=Decimal(49000), credit=Decimal(500), bonus=Decimal(0), order=1),
        dict(code="standard", name="بستهٔ استاندارد", price=Decimal(99000), credit=Decimal(1200), bonus=Decimal(100), order=2),
        dict(code="power", name="بستهٔ قدرت", price=Decimal(199000), credit=Decimal(2800), bonus=Decimal(400), order=3),
    ]
    for p in packs:
        existing = (
            await db.execute(select(CreditPackage).where(CreditPackage.code == p["code"]))
        ).scalar_one_or_none()
        if existing:
            continue
        db.add(
            CreditPackage(
                code=p["code"], name=p["name"], price=p["price"],
                credit_amount=p["credit"], bonus_credit=p["bonus"], display_order=p["order"],
            )
        )


async def _seed_config(db):
    configs = {
        "free_message_limit": {"value": 20},
        "signup_bonus": {"value": 100},
        "daily_bonus": {"value": 10},
        "maintenance_mode": {"value": False},
        "minimum_app_version": {"value": "1.0.0"},
    }
    for key, value in configs.items():
        existing = (
            await db.execute(select(RemoteConfig).where(RemoteConfig.key == key))
        ).scalar_one_or_none()
        if existing is None:
            db.add(RemoteConfig(key=key, value=value))

    flags = ["voice_enabled", "video_enabled", "web_search_enabled", "memory_enabled", "referral_enabled"]
    for key in flags:
        existing = (
            await db.execute(select(FeatureFlag).where(FeatureFlag.key == key))
        ).scalar_one_or_none()
        if existing is None:
            db.add(FeatureFlag(key=key, enabled=False))


async def _seed_admin(db):
    email = "admin@deepgap.local"
    existing = (
        await db.execute(select(AdminUser).where(AdminUser.email == email))
    ).scalar_one_or_none()
    if existing is None:
        db.add(
            AdminUser(
                email=email,
                password_hash=hash_password("change-me-admin"),
                role="super_admin",
            )
        )


async def seed() -> None:
    async with SessionLocal() as db:
        provider = await _seed_providers(db)
        await _seed_models(db, provider)
        await _seed_plans(db)
        await _seed_credit_packs(db)
        await _seed_config(db)
        await _seed_admin(db)
        await db.commit()
    print("✅ Seed complete.")


if __name__ == "__main__":
    asyncio.run(seed())
