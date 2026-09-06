"""Auth service: user provisioning, token issuance and refresh rotation."""
from __future__ import annotations

import secrets
import string
import uuid
from datetime import UTC, datetime, timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import AuthError
from app.core.security import (
    JWTError,
    create_access_token,
    create_refresh_token,
    decode_token,
)
from app.models.subscription import SubscriptionPlan
from app.models.user import Session, User, UserProfile
from app.services.wallet_service import WalletService

_ALPHABET = string.ascii_uppercase + string.digits


def _gen_referral_code() -> str:
    return "".join(secrets.choice(_ALPHABET) for _ in range(8))


class AuthService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db

    async def get_by_phone(self, phone: str) -> User | None:
        return (
            await self.db.execute(select(User).where(User.phone == phone))
        ).scalar_one_or_none()

    async def get_or_create_user(self, phone: str) -> tuple[User, bool]:
        user = await self.get_by_phone(phone)
        if user:
            return user, False

        # Ensure a unique referral code.
        code = _gen_referral_code()
        while (
            await self.db.execute(select(User).where(User.referral_code == code))
        ).scalar_one_or_none():
            code = _gen_referral_code()

        user = User(phone=phone, referral_code=code, is_phone_verified=True)
        self.db.add(user)
        await self.db.flush()

        self.db.add(UserProfile(user_id=user.id))

        # Signup bonus (amount controlled by remote config / plan in real setup).
        wallet = WalletService(self.db)
        await wallet.get_or_create(user.id)
        signup_bonus = await self._signup_bonus()
        if signup_bonus > 0:
            await wallet.credit(
                user.id,
                signup_bonus,
                type="bonus",
                reason="signup_bonus",
                is_bonus=True,
                idempotency_key=f"signup:{user.id}",
            )
        await self.db.flush()
        return user, True

    async def _signup_bonus(self):
        from decimal import Decimal

        # Could be sourced from RemoteConfig; sensible default for MVP.
        return Decimal(100)

    async def issue_tokens(
        self,
        user: User,
        *,
        device_id: str | None = None,
        ip: str | None = None,
        user_agent: str | None = None,
    ) -> dict:
        access = create_access_token(str(user.id))
        refresh, jti = create_refresh_token(str(user.id))
        expires_at = datetime.now(UTC) + timedelta(
            days=settings.refresh_token_expire_days
        )
        self.db.add(
            Session(
                user_id=user.id,
                device_id=device_id,
                refresh_jti=jti,
                expires_at=expires_at,
                ip_address=ip,
                user_agent=user_agent,
            )
        )
        user.last_login_at = datetime.now(UTC)
        await self.db.flush()
        return {
            "access_token": access,
            "refresh_token": refresh,
            "token_type": "bearer",
            "expires_in": settings.access_token_expire_minutes * 60,
        }

    async def refresh(self, refresh_token: str) -> dict:
        try:
            payload = decode_token(refresh_token)
        except JWTError as exc:
            raise AuthError("توکن نامعتبر است") from exc
        if payload.get("type") != "refresh":
            raise AuthError("نوع توکن نامعتبر است")

        jti = payload.get("jti")
        session = (
            await self.db.execute(select(Session).where(Session.refresh_jti == jti))
        ).scalar_one_or_none()
        if session is None or session.revoked_at is not None:
            raise AuthError("نشست نامعتبر یا باطل‌شده است")
        if session.expires_at < datetime.now(UTC):
            raise AuthError("نشست منقضی شده است")

        user = await self.db.get(User, uuid.UUID(payload["sub"]))
        if user is None or user.is_blocked or not user.is_active:
            raise AuthError("دسترسی مجاز نیست")

        # Rotate: revoke old session, issue a fresh one.
        session.revoked_at = datetime.now(UTC)
        return await self.issue_tokens(user, device_id=session.device_id)

    async def logout(self, jti: str) -> None:
        session = (
            await self.db.execute(select(Session).where(Session.refresh_jti == jti))
        ).scalar_one_or_none()
        if session and session.revoked_at is None:
            session.revoked_at = datetime.now(UTC)
            await self.db.flush()


async def get_current_user(db: AsyncSession, token: str) -> User:
    try:
        payload = decode_token(token)
    except JWTError as exc:
        raise AuthError("توکن نامعتبر است") from exc
    if payload.get("type") != "access":
        raise AuthError("نوع توکن نامعتبر است")
    user = await db.get(User, uuid.UUID(payload["sub"]))
    if user is None or user.is_blocked or not user.is_active:
        raise AuthError("دسترسی مجاز نیست")
    return user


async def user_tier(db: AsyncSession, user: User) -> str:
    """Resolve the user's active subscription tier (defaults to free)."""
    from app.models.subscription import Subscription

    stmt = (
        select(SubscriptionPlan.code)
        .join(Subscription, Subscription.plan_id == SubscriptionPlan.id)
        .where(
            Subscription.user_id == user.id,
            Subscription.status == "active",
            Subscription.expires_at > datetime.now(UTC),
        )
        .order_by(SubscriptionPlan.display_order.desc())
        .limit(1)
    )
    tier = (await db.execute(stmt)).scalar_one_or_none()
    return tier or "free"
