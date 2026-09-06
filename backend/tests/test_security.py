"""Auth/security unit tests."""
import pytest

from app.core.security import (
    JWTError,
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.schemas.auth import OTPRequest


def test_password_hash_roundtrip():
    hashed = hash_password("s3cret-pass")
    assert hashed != "s3cret-pass"
    assert verify_password("s3cret-pass", hashed)
    assert not verify_password("wrong", hashed)


def test_access_token_roundtrip():
    token = create_access_token("user-123")
    payload = decode_token(token)
    assert payload["sub"] == "user-123"
    assert payload["type"] == "access"


def test_refresh_token_has_jti():
    token, jti = create_refresh_token("user-123")
    payload = decode_token(token)
    assert payload["type"] == "refresh"
    assert payload["jti"] == jti


def test_invalid_token_raises():
    with pytest.raises(JWTError):
        decode_token("not-a-real-token")


def test_phone_validation():
    assert OTPRequest(phone="09121234567").phone == "09121234567"
    with pytest.raises(ValueError):
        OTPRequest(phone="12345")
