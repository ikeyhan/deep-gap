from __future__ import annotations

from fastapi import APIRouter
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.models.subscription import CreditPackage, SubscriptionPlan
from app.schemas.billing import CreditPackageOut, PlanOut, SubscriptionOut
from app.services.subscription_service import SubscriptionService

router = APIRouter(prefix="/subscriptions", tags=["subscriptions"])


@router.get("/plans", response_model=list[PlanOut])
async def list_plans(db: DbDep):
    stmt = (
        select(SubscriptionPlan)
        .where(SubscriptionPlan.is_active.is_(True))
        .order_by(SubscriptionPlan.display_order.asc())
    )
    rows = (await db.execute(stmt)).scalars().all()
    return [PlanOut.model_validate(p) for p in rows]


@router.get("/packages", response_model=list[CreditPackageOut])
async def list_packages(db: DbDep):
    stmt = (
        select(CreditPackage)
        .where(CreditPackage.is_active.is_(True))
        .order_by(CreditPackage.display_order.asc())
    )
    rows = (await db.execute(stmt)).scalars().all()
    return [CreditPackageOut.model_validate(p) for p in rows]


@router.get("/me", response_model=SubscriptionOut | None)
async def my_subscription(user: CurrentUser, db: DbDep):
    sub = await SubscriptionService(db).active_subscription(user.id)
    return SubscriptionOut.model_validate(sub) if sub else None
