from __future__ import annotations

from fastapi import APIRouter, Request

from app.core.config import settings
from app.core.deps import DbDep, RedisDep
from app.core.exceptions import AuthError
from app.schemas.auth import (
    OTPRequest,
    OTPRequestResponse,
    OTPVerify,
    RefreshRequest,
    TokenResponse,
)
from app.services.auth_service import AuthService
from app.services.otp_service import OTPService

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/otp/request", response_model=OTPRequestResponse)
async def request_otp(payload: OTPRequest, redis: RedisDep) -> OTPRequestResponse:
    otp = OTPService(redis)
    dev_code = await otp.request(payload.phone)
    return OTPRequestResponse(
        message="کد تأیید ارسال شد",
        dev_code=dev_code if settings.app_env != "production" else None,
    )


@router.post("/otp/verify", response_model=TokenResponse)
async def verify_otp(payload: OTPVerify, db: DbDep, redis: RedisDep, request: Request) -> TokenResponse:
    otp = OTPService(redis)
    if not await otp.verify(payload.phone, payload.code):
        raise AuthError("کد تأیید نادرست است", error_code="otp_invalid")

    auth = AuthService(db)
    user, is_new = await auth.get_or_create_user(payload.phone)
    if user.is_blocked:
        raise AuthError("حساب شما مسدود شده است", error_code="account_blocked")

    tokens = await auth.issue_tokens(
        user,
        device_id=payload.device_id,
        ip=request.client.host if request.client else None,
        user_agent=request.headers.get("user-agent"),
    )
    await db.commit()
    return TokenResponse(**tokens, is_new_user=is_new)


@router.post("/refresh", response_model=TokenResponse)
async def refresh(payload: RefreshRequest, db: DbDep) -> TokenResponse:
    auth = AuthService(db)
    tokens = await auth.refresh(payload.refresh_token)
    await db.commit()
    return TokenResponse(**tokens)
