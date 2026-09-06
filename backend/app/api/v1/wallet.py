from __future__ import annotations

from fastapi import APIRouter
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.models.wallet import WalletTransaction
from app.schemas.wallet import TransactionOut, WalletOut
from app.services.wallet_service import WalletService

router = APIRouter(prefix="/wallet", tags=["wallet"])


@router.get("", response_model=WalletOut)
async def get_wallet(user: CurrentUser, db: DbDep) -> WalletOut:
    wallet = await WalletService(db).get_or_create(user.id)
    await db.commit()
    return WalletOut.model_validate(wallet)


@router.get("/transactions", response_model=list[TransactionOut])
async def transactions(user: CurrentUser, db: DbDep, limit: int = 50, offset: int = 0):
    wallet = await WalletService(db).get_or_create(user.id)
    stmt = (
        select(WalletTransaction)
        .where(WalletTransaction.wallet_id == wallet.id)
        .order_by(WalletTransaction.created_at.desc())
        .limit(min(limit, 200))
        .offset(offset)
    )
    rows = (await db.execute(stmt)).scalars().all()
    return [TransactionOut.model_validate(r) for r in rows]
