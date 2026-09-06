"""FastAPI dependencies: DB session, Redis, current authenticated user."""
from __future__ import annotations

from typing import Annotated

import redis.asyncio as aioredis
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.redis import get_redis
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
