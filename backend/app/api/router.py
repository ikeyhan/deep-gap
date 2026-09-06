"""Aggregate all v1 routers under the API prefix."""
from __future__ import annotations

from fastapi import APIRouter

from app.api.v1 import auth, chat, health, models, users, wallet

api_router = APIRouter()
api_router.include_router(health.router)
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(models.router)
api_router.include_router(chat.router)
api_router.include_router(wallet.router)
