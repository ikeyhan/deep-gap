from __future__ import annotations

import uuid
from datetime import datetime

from fastapi import APIRouter
from pydantic import BaseModel

from app.core.deps import CurrentUser, DbDep
from app.schemas.common import Message
from app.services.auth_service import user_tier

router = APIRouter(prefix="/users", tags=["users"])


class MeResponse(BaseModel):
    id: uuid.UUID
    phone: str
    referral_code: str
    tier: str
    created_at: datetime


@router.get("/me", response_model=MeResponse)
async def me(user: CurrentUser, db: DbDep) -> MeResponse:
    tier = await user_tier(db, user)
    return MeResponse(
        id=user.id,
        phone=user.phone,
        referral_code=user.referral_code,
        tier=tier,
        created_at=user.created_at,
    )


@router.delete("/me", response_model=Message)
async def delete_account(user: CurrentUser, db: DbDep) -> Message:
    """Privacy by design (spec rule 41): soft-delete the account."""
    user.deleted_at = datetime.utcnow()
    user.is_active = False
    await db.commit()
    return Message(message="حساب کاربری برای حذف علامت‌گذاری شد")
