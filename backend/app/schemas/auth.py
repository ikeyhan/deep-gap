from __future__ import annotations

import re

from pydantic import BaseModel, field_validator

_PHONE_RE = re.compile(r"^09\d{9}$")


class OTPRequest(BaseModel):
    phone: str

    @field_validator("phone")
    @classmethod
    def valid_phone(cls, v: str) -> str:
        v = v.strip()
        if not _PHONE_RE.match(v):
            raise ValueError("شماره موبایل نامعتبر است (نمونه: 09xxxxxxxxx)")
        return v


class OTPVerify(BaseModel):
    phone: str
    code: str
    device_id: str | None = None


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    is_new_user: bool = False


class RefreshRequest(BaseModel):
    refresh_token: str


class OTPRequestResponse(BaseModel):
    message: str
    # Present only in development (console provider) to ease testing.
    dev_code: str | None = None
