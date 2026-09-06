"""Shared response schemas including the standard error envelope (spec rule 48)."""
from __future__ import annotations

from typing import Any

from pydantic import BaseModel


class ErrorEnvelope(BaseModel):
    error_code: str
    message: str
    details: Any | None = None
    request_id: str | None = None
    timestamp: str


class Message(BaseModel):
    message: str


class Page(BaseModel):
    total: int
    items: list[Any]
