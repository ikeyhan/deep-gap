"""Deep Gap (دیپ گپ) — FastAPI application entrypoint."""
from __future__ import annotations

import uuid
from contextlib import asynccontextmanager
from datetime import UTC, datetime

import sentry_sdk
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.api.router import api_router
from app.core.config import settings
from app.core.exceptions import AppError
from app.core.logging import configure_logging, get_logger
from app.services.ai.registry import bootstrap_providers
from app.services.payments.registry import bootstrap_payment_providers

log = get_logger("app")


@asynccontextmanager
async def lifespan(_: FastAPI):
    configure_logging()
    bootstrap_providers()
    bootstrap_payment_providers()
    if settings.sentry_dsn:
        sentry_sdk.init(dsn=settings.sentry_dsn, environment=settings.app_env)
    log.info("app_started", env=settings.app_env)
    yield
    log.info("app_stopped")


app = FastAPI(
    title="Deep Gap API",
    version="0.1.0",
    description="پلتفرم هوش مصنوعی چندمدلی دیپ گپ — Backend API",
    docs_url="/docs",
    openapi_url="/openapi.json",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def add_request_id(request: Request, call_next):
    request_id = request.headers.get("x-request-id", str(uuid.uuid4()))
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["x-request-id"] = request_id
    return response


def _error_response(request: Request, status_code: int, error_code: str, message: str, details=None):
    return JSONResponse(
        status_code=status_code,
        content={
            "error_code": error_code,
            "message": message,
            "details": details,
            "request_id": getattr(request.state, "request_id", None),
            "timestamp": datetime.now(UTC).isoformat(),
        },
    )


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError):
    return _error_response(request, exc.status_code, exc.error_code, exc.message, exc.details)


@app.exception_handler(Exception)
async def unhandled_handler(request: Request, exc: Exception):
    log.error("unhandled_error", error=str(exc), path=request.url.path)
    return _error_response(
        request, 500, "internal_error", "خطای داخلی سرور رخ داد"
    )


app.include_router(api_router, prefix=settings.api_v1_prefix)


@app.get("/")
async def root() -> dict:
    return {"service": "Deep Gap API", "version": "0.1.0", "docs": "/docs"}
