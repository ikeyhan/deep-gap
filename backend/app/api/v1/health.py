from __future__ import annotations

from fastapi import APIRouter
from sqlalchemy import text

from app.core.deps import DbDep, RedisDep

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> dict:
    return {"status": "ok", "service": "deepgap-backend"}


@router.get("/health/ready")
async def ready(db: DbDep, redis: RedisDep) -> dict:
    checks = {"database": False, "redis": False}
    try:
        await db.execute(text("SELECT 1"))
        checks["database"] = True
    except Exception:  # noqa: BLE001
        pass
    try:
        await redis.ping()
        checks["redis"] = True
    except Exception:  # noqa: BLE001
        pass
    status = "ok" if all(checks.values()) else "degraded"
    return {"status": status, "checks": checks}
