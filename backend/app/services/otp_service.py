"""OTP service backed by Redis with rate limiting (spec rules 39, 40, 76)."""
from __future__ import annotations

import secrets

import redis.asyncio as aioredis

from app.core.config import settings
from app.core.exceptions import RateLimitError, ValidationError
from app.core.logging import get_logger

log = get_logger("otp")


def _code_key(phone: str) -> str:
    return f"otp:code:{phone}"


def _attempts_key(phone: str) -> str:
    return f"otp:attempts:{phone}"


def _cooldown_key(phone: str) -> str:
    return f"otp:cooldown:{phone}"


class OTPService:
    def __init__(self, redis: aioredis.Redis) -> None:
        self.redis = redis

    async def request(self, phone: str) -> str | None:
        # Resend cooldown.
        if await self.redis.exists(_cooldown_key(phone)):
            raise RateLimitError("لطفاً کمی صبر کنید و دوباره تلاش کنید")

        code = "".join(secrets.choice("0123456789") for _ in range(settings.otp_length))
        await self.redis.set(_code_key(phone), code, ex=settings.otp_ttl_seconds)
        await self.redis.delete(_attempts_key(phone))
        await self.redis.set(
            _cooldown_key(phone), "1", ex=settings.otp_resend_cooldown_seconds
        )

        if settings.otp_provider == "console":
            # Dev only: log the code (never in production).
            log.info("otp_generated", phone=phone, dev_hint=code)
            return code
        # TODO integration point: call real SMS provider here.
        return None

    async def verify(self, phone: str, code: str) -> bool:
        stored = await self.redis.get(_code_key(phone))
        if stored is None:
            raise ValidationError("کد منقضی شده است؛ دوباره درخواست دهید", error_code="otp_expired")

        attempts = await self.redis.incr(_attempts_key(phone))
        if attempts == 1:
            await self.redis.expire(_attempts_key(phone), settings.otp_ttl_seconds)
        if attempts > settings.otp_max_attempts:
            await self.redis.delete(_code_key(phone))
            raise RateLimitError("تعداد تلاش‌های مجاز تمام شد", error_code="otp_too_many_attempts")

        if not secrets.compare_digest(stored, code):
            return False

        await self.redis.delete(_code_key(phone), _attempts_key(phone))
        return True
