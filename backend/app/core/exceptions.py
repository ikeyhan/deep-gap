"""Application-wide exceptions and a standard error envelope (spec rule 48)."""
from __future__ import annotations

from typing import Any


class AppError(Exception):
    """Base class for expected, user-facing application errors."""

    status_code: int = 400
    error_code: str = "app_error"

    def __init__(
        self,
        message: str,
        *,
        error_code: str | None = None,
        status_code: int | None = None,
        details: Any = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        if error_code:
            self.error_code = error_code
        if status_code:
            self.status_code = status_code
        self.details = details


class NotFoundError(AppError):
    status_code = 404
    error_code = "not_found"


class AuthError(AppError):
    status_code = 401
    error_code = "unauthorized"


class ForbiddenError(AppError):
    status_code = 403
    error_code = "forbidden"


class ValidationError(AppError):
    status_code = 422
    error_code = "validation_error"


class RateLimitError(AppError):
    status_code = 429
    error_code = "rate_limited"


class InsufficientCreditError(AppError):
    status_code = 402
    error_code = "insufficient_credit"


class ProviderError(AppError):
    status_code = 502
    error_code = "provider_error"
