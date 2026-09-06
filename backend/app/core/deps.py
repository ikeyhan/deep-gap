"""FastAPI dependencies: DB session, Redis, current authenticated user."""
from __future__ import annotations

import uuid
from collections.abc import Callable
from typing import Annotated

import redis.asyncio as aioredis
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.exceptions import AuthError, ForbiddenError
from app.core.redis import get_redis
from app.core.security import JWTError, decode_token
from app.models.platform import AdminUser
from app.models.user import User
from app.services.auth_service import get_current_user

bearer_scheme = HTTPBearer(auto_error=True)

DbDep = Annotated[AsyncSession, Depends(get_db)]
RedisDep = Annotated[aioredis.Redis, Depends(get_redis)]


async def current_user(
    db: DbDep,
    creds: Annotated[HTTPAuthorizationCredentials, Depends(bearer_scheme)],
) -> User:
    return await get_current_user(db, creds.credentials)


CurrentUser = Annotated[User, Depends(current_user)]


async def current_admin(
    db: DbDep,
    creds: Annotated[HTTPAuthorizationCredentials, Depends(bearer_scheme)],
) -> AdminUser:
    """Admin auth is fully separate from user auth (spec rule 77)."""
    try:
        payload = decode_token(creds.credentials)
    except JWTError as exc:
        raise AuthError("توکن نامعتبر است") from exc
    if payload.get("scope") != "admin":
        raise ForbiddenError("دسترسی ادمین لازم است", error_code="admin_required")
    admin = await db.get(AdminUser, uuid.UUID(payload["sub"]))
    if admin is None or not admin.is_active:
        raise AuthError("دسترسی مجاز نیست")
    return admin


CurrentAdmin = Annotated[AdminUser, Depends(current_admin)]


def require_roles(*roles: str) -> Callable:
    """RBAC guard. super_admin always passes (spec rule 78)."""

    async def _guard(admin: CurrentAdmin) -> AdminUser:
        if admin.role != "super_admin" and admin.role not in roles:
            raise ForbiddenError("سطح دسترسی کافی نیست", error_code="insufficient_role")
        return admin

    return _guard
