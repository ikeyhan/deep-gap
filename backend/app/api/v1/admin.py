from __future__ import annotations

import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy import or_, select
from sqlalchemy.orm import selectinload

from app.core.deps import CurrentAdmin, DbDep, require_roles
from app.core.exceptions import NotFoundError, ValidationError
from app.models.ai import AIModel, AIProviderRow, ModelPricing
from app.models.user import User
from app.schemas.admin import (
    AdminLogin,
    AdminModelOut,
    AdminToken,
    AdminUserOut,
    CreditAdjustIn,
    ModelCreate,
    ModelUpdate,
)
from app.schemas.common import Message
from app.services.admin_service import AdminService

router = APIRouter(prefix="/admin", tags=["admin"])


# ------------------------------------------------------------------ auth
@router.post("/auth/login", response_model=AdminToken)
async def admin_login(payload: AdminLogin, db: DbDep):
    service = AdminService(db)
    token = await service.login(payload.email, payload.password)
    # Role is echoed for the panel UI; authoritative role lives in the token.
    from app.models.platform import AdminUser

    admin = (
        await db.execute(select(AdminUser).where(AdminUser.email == payload.email))
    ).scalar_one()
    return AdminToken(access_token=token, role=admin.role)


# ------------------------------------------------------------------ dashboard
@router.get("/dashboard", dependencies=[Depends(require_roles("finance", "analyst"))])
async def dashboard(db: DbDep):
    return await AdminService(db).dashboard()


@router.get("/finance/top-models", dependencies=[Depends(require_roles("finance", "analyst"))])
async def top_models(db: DbDep, limit: int = 10):
    return await AdminService(db).top_models(limit=min(limit, 50))


# ------------------------------------------------------------------ models
@router.get("/models", response_model=list[AdminModelOut])
async def list_models(admin: CurrentAdmin, db: DbDep):
    rows = (
        await db.execute(
            select(AIModel).where(AIModel.deleted_at.is_(None)).order_by(AIModel.display_order)
        )
    ).scalars().all()
    return [AdminModelOut.model_validate(m) for m in rows]


@router.post("/models", response_model=AdminModelOut, status_code=201,
             dependencies=[Depends(require_roles("content"))])
async def create_model(payload: ModelCreate, admin: CurrentAdmin, db: DbDep):
    provider = (
        await db.execute(select(AIProviderRow).where(AIProviderRow.key == payload.provider_key))
    ).scalar_one_or_none()
    if provider is None:
        raise NotFoundError("ارائه‌دهنده یافت نشد", error_code="provider_not_found")
    exists = (
        await db.execute(select(AIModel).where(AIModel.code == payload.code))
    ).scalar_one_or_none()
    if exists:
        raise ValidationError("کد مدل تکراری است", error_code="model_code_exists")

    model = AIModel(
        provider_id=provider.id,
        provider_model=payload.provider_model,
        code=payload.code,
        display_name=payload.display_name,
        description=payload.description,
        capability=payload.capability,
        is_premium=payload.is_premium,
        min_tier=payload.min_tier,
        display_order=payload.display_order,
        max_tokens=payload.max_tokens,
        fallback_codes=payload.fallback_codes,
    )
    db.add(model)
    await db.flush()
    db.add(ModelPricing(model_id=model.id, **payload.pricing.model_dump()))

    await AdminService(db).audit(
        admin=admin, action="create_model", target_type="ai_model", target_id=str(model.id)
    )
    await db.commit()
    await db.refresh(model)
    return AdminModelOut.model_validate(model)


@router.patch("/models/{model_id}", response_model=AdminModelOut,
              dependencies=[Depends(require_roles("content"))])
async def update_model(model_id: uuid.UUID, payload: ModelUpdate, admin: CurrentAdmin, db: DbDep):
    model = (
        await db.execute(
            select(AIModel).options(selectinload(AIModel.pricing)).where(AIModel.id == model_id)
        )
    ).scalar_one_or_none()
    if model is None:
        raise NotFoundError("مدل یافت نشد", error_code="model_not_found")

    data = payload.model_dump(exclude_unset=True)
    pricing_data = data.pop("pricing", None)
    for field, value in data.items():
        setattr(model, field, value)

    if pricing_data:
        pricing = model.pricing or ModelPricing(model_id=model.id)
        for field, value in pricing_data.items():
            setattr(pricing, field, value)
        db.add(pricing)

    await AdminService(db).audit(
        admin=admin, action="update_model", target_type="ai_model",
        target_id=str(model.id), meta={"changes": list(data.keys())},
    )
    await db.commit()
    await db.refresh(model)
    return AdminModelOut.model_validate(model)


@router.post("/models/{model_id}/toggle", response_model=AdminModelOut,
             dependencies=[Depends(require_roles("content"))])
async def toggle_model(model_id: uuid.UUID, admin: CurrentAdmin, db: DbDep, enabled: bool = Query(...)):
    model = await db.get(AIModel, model_id)
    if model is None:
        raise NotFoundError("مدل یافت نشد", error_code="model_not_found")
    model.is_enabled = enabled
    await AdminService(db).audit(
        admin=admin, action="toggle_model", target_type="ai_model",
        target_id=str(model.id), meta={"enabled": enabled},
    )
    await db.commit()
    await db.refresh(model)
    return AdminModelOut.model_validate(model)


# ------------------------------------------------------------------ users
@router.get("/users", response_model=list[AdminUserOut],
            dependencies=[Depends(require_roles("support"))])
async def search_users(db: DbDep, q: str | None = None, limit: int = 50, offset: int = 0):
    stmt = select(User).where(User.deleted_at.is_(None))
    if q:
        stmt = stmt.where(or_(User.phone.ilike(f"%{q}%"), User.referral_code.ilike(f"%{q}%")))
    stmt = stmt.order_by(User.created_at.desc()).limit(min(limit, 100)).offset(offset)
    rows = (await db.execute(stmt)).scalars().all()
    return [AdminUserOut.model_validate(u) for u in rows]


@router.post("/users/{user_id}/block", response_model=Message,
             dependencies=[Depends(require_roles("support"))])
async def block_user(user_id: uuid.UUID, admin: CurrentAdmin, db: DbDep, blocked: bool = Query(True)):
    user = await db.get(User, user_id)
    if user is None:
        raise NotFoundError("کاربر یافت نشد", error_code="user_not_found")
    user.is_blocked = blocked
    await AdminService(db).audit(
        admin=admin, action="block_user" if blocked else "unblock_user",
        target_type="user", target_id=str(user_id),
    )
    await db.commit()
    return Message(message="کاربر مسدود شد" if blocked else "کاربر آزاد شد")


@router.post("/users/{user_id}/adjust-credit", response_model=Message,
             dependencies=[Depends(require_roles("finance"))])
async def adjust_credit(user_id: uuid.UUID, payload: CreditAdjustIn, admin: CurrentAdmin, db: DbDep):
    user = await db.get(User, user_id)
    if user is None:
        raise NotFoundError("کاربر یافت نشد", error_code="user_not_found")
    balance = await AdminService(db).adjust_credit(
        admin=admin, user_id=user_id, amount=payload.amount, reason=payload.reason
    )
    await db.commit()
    return Message(message=f"موجودی به‌روزرسانی شد: {balance}")
