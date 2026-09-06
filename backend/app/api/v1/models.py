from __future__ import annotations

from fastapi import APIRouter
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.models.ai import AIModel
from app.schemas.wallet import ModelOut
from app.services.auth_service import user_tier

router = APIRouter(prefix="/models", tags=["models"])

_TIER_RANK = {"free": 0, "plus": 1, "pro": 2}


@router.get("", response_model=list[ModelOut])
async def list_models(user: CurrentUser, db: DbDep):
    """Public catalogue of enabled models, ordered for the Home screen."""
    tier = await user_tier(db, user)
    stmt = (
        select(AIModel)
        .where(AIModel.is_enabled.is_(True), AIModel.deleted_at.is_(None))
        .order_by(AIModel.display_order.asc())
    )
    models = (await db.execute(stmt)).scalars().all()
    # Every enabled model is listed; the client shows a lock on premium/tier-gated ones.
    _ = _TIER_RANK.get(tier, 0)
    return [ModelOut.model_validate(m) for m in models]
