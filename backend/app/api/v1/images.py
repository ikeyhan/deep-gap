from __future__ import annotations

import time
import uuid

from fastapi import APIRouter
from sqlalchemy import select

from app.core.deps import CurrentUser, DbDep
from app.core.exceptions import InsufficientCreditError, ProviderError, ValidationError
from app.core.logging import get_logger
from app.models.ai import ModelPricing
from app.schemas.images import ImageGenerateRequest, ImageGenerateResponse
from app.services.ai.base import ImageRequest
from app.services.ai.router import AIRouter
from app.services.auth_service import user_tier
from app.services.billing_service import BillingService
from app.services.wallet_service import WalletService

router = APIRouter(prefix="/images", tags=["images"])
log = get_logger("images")


@router.post("/generate", response_model=ImageGenerateResponse)
async def generate_image(payload: ImageGenerateRequest, user: CurrentUser, db: DbDep):
    tier = await user_tier(db, user)
    resolved = await AIRouter(db).resolve(payload.model_code, user_tier=tier)

    if resolved.model.capability != "image":
        raise ValidationError(
            "این مدل برای تولید تصویر نیست", error_code="not_an_image_model"
        )
    if not resolved.adapter.supports(_image_capability()):
        raise ProviderError("ارائه‌دهنده از تولید تصویر پشتیبانی نمی‌کند")

    pricing = (
        await db.execute(select(ModelPricing).where(ModelPricing.model_id == resolved.model.id))
    ).scalar_one_or_none()
    if pricing is None:
        raise ProviderError("قیمت مدل تعریف نشده است", error_code="pricing_missing")

    billing = BillingService(db)
    wallet = await WalletService(db).get_or_create(user.id)
    # Unit-based pre-flight: charge scales with number of images requested.
    est = (pricing.user_price_unit * payload.n) or pricing.minimum_charge
    if wallet.balance < est:
        raise InsufficientCreditError("اعتبار کافی برای تولید تصویر ندارید")

    request_id = str(uuid.uuid4())
    usage_log = await billing.open_usage(
        user_id=user.id, model=resolved.model, request_type="image", request_id=request_id
    )

    started = time.perf_counter()
    try:
        result = await resolved.adapter.generate_image(
            ImageRequest(
                model=resolved.model.provider_model,
                prompt=payload.prompt,
                size=payload.size,
                n=payload.n,
                request_id=request_id,
            )
        )
    except Exception as exc:  # noqa: BLE001
        await billing.fail_usage(log=usage_log, reason=str(exc))
        await db.commit()
        log.warning("image_provider_failed", request_id=request_id, error=str(exc))
        raise ProviderError("خطا در تولید تصویر") from exc

    latency_ms = int((time.perf_counter() - started) * 1000)
    charge = await billing.settle_usage(
        log=usage_log, pricing=pricing, usage=result.usage, latency_ms=latency_ms, units=payload.n
    )

    wallet = await WalletService(db).get_or_create(user.id)
    await db.commit()
    return ImageGenerateResponse(
        urls=result.urls,
        model_code=payload.model_code,
        charged_credit=float(charge),
        balance=float(wallet.balance),
    )


def _image_capability():
    from app.services.ai.base import Capability

    return Capability.IMAGE
