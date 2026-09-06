"""Admin service: authentication, audit logging, credit adjustment, analytics."""
from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AuthError
from app.core.security import create_access_token, verify_password
from app.models.ai import AIModel, UsageLog
from app.models.platform import AdminUser, AuditLog, Payment
from app.models.user import User
from app.services.wallet_service import WalletService


class AdminService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db

    # ---- auth ----
    async def login(self, email: str, password: str) -> str:
        admin = (
            await self.db.execute(select(AdminUser).where(AdminUser.email == email))
        ).scalar_one_or_none()
        if admin is None or not admin.is_active or not verify_password(password, admin.password_hash):
            raise AuthError("ایمیل یا رمز عبور نادرست است", error_code="admin_login_failed")
        return create_access_token(str(admin.id), extra={"scope": "admin", "role": admin.role})

    # ---- audit (spec rules 77, 105) ----
    async def audit(
        self,
        *,
        admin: AdminUser,
        action: str,
        target_type: str | None = None,
        target_id: str | None = None,
        meta: dict | None = None,
        ip: str | None = None,
    ) -> None:
        self.db.add(
            AuditLog(
                actor_type="admin",
                actor_id=admin.id,
                action=action,
                target_type=target_type,
                target_id=target_id,
                meta=meta,
                ip_address=ip,
            )
        )
        await self.db.flush()

    # ---- credit adjustment (audited, ledgered) ----
    async def adjust_credit(
        self, *, admin: AdminUser, user_id: uuid.UUID, amount: Decimal, reason: str
    ) -> Decimal:
        wallet = WalletService(self.db)
        w = await wallet.get_or_create(user_id)
        if amount >= 0:
            await wallet.credit(
                user_id, amount, type="adjustment",
                reason=f"admin_adjust:{reason}", reference_type="admin", reference_id=str(admin.id),
            )
        else:
            await wallet.debit(
                user_id, -amount, type="adjustment",
                reason=f"admin_adjust:{reason}", reference_type="admin", reference_id=str(admin.id),
            )
        await self.audit(
            admin=admin, action="adjust_credit", target_type="user",
            target_id=str(user_id), meta={"amount": str(amount), "reason": reason},
        )
        w = await wallet.get_or_create(user_id)
        return w.balance

    # ---- analytics (spec rules 29, 32) ----
    async def dashboard(self) -> dict:
        now = datetime.now(UTC)
        d7 = now - timedelta(days=7)
        d30 = now - timedelta(days=30)

        total_users = (
            await self.db.execute(
                select(func.count()).select_from(User).where(User.deleted_at.is_(None))
            )
        ).scalar_one()
        new_users_7d = (
            await self.db.execute(
                select(func.count()).select_from(User).where(User.created_at >= d7)
            )
        ).scalar_one()
        paid_users = (
            await self.db.execute(
                select(func.count(func.distinct(Payment.user_id))).where(
                    Payment.status == "verified"
                )
            )
        ).scalar_one()

        revenue_30d = (
            await self.db.execute(
                select(func.coalesce(func.sum(Payment.amount), 0)).where(
                    Payment.status == "verified", Payment.verified_at >= d30
                )
            )
        ).scalar_one()
        ai_cost_30d = (
            await self.db.execute(
                select(func.coalesce(func.sum(UsageLog.provider_cost), 0)).where(
                    UsageLog.status == "completed", UsageLog.created_at >= d30
                )
            )
        ).scalar_one()
        gross_profit_30d = (
            await self.db.execute(
                select(func.coalesce(func.sum(UsageLog.gross_profit), 0)).where(
                    UsageLog.status == "completed", UsageLog.created_at >= d30
                )
            )
        ).scalar_one()
        requests_30d = (
            await self.db.execute(
                select(func.count()).select_from(UsageLog).where(UsageLog.created_at >= d30)
            )
        ).scalar_one()

        conversion = (paid_users / total_users) if total_users else 0.0
        return {
            "total_users": total_users,
            "new_users_7d": new_users_7d,
            "paid_users": paid_users,
            "revenue_30d": str(revenue_30d),
            "ai_cost_30d": str(ai_cost_30d),
            "gross_profit_30d": str(gross_profit_30d),
            "requests_30d": requests_30d,
            "conversion_rate": round(conversion, 4),
        }

    async def top_models(self, limit: int = 10) -> list[dict]:
        stmt = (
            select(
                AIModel.code,
                AIModel.display_name,
                func.count(UsageLog.id).label("requests"),
                func.coalesce(func.sum(UsageLog.charged_credit), 0).label("revenue"),
                func.coalesce(func.sum(UsageLog.gross_profit), 0).label("profit"),
            )
            .join(UsageLog, UsageLog.model_id == AIModel.id)
            .where(UsageLog.status == "completed")
            .group_by(AIModel.code, AIModel.display_name)
            .order_by(func.count(UsageLog.id).desc())
            .limit(limit)
        )
        rows = (await self.db.execute(stmt)).all()
        return [
            {
                "code": r.code,
                "display_name": r.display_name,
                "requests": r.requests,
                "revenue": str(r.revenue),
                "profit": str(r.profit),
            }
            for r in rows
        ]
