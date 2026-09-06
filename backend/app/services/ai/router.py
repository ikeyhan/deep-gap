"""Smart routing (spec rule 9).

Resolves a public model code (e.g. "fast", "smart") to a concrete DB model +
provider adapter, honouring enabled state, tier and region. On provider failure
the caller can walk the model's fallback_codes.
"""
from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import ForbiddenError, NotFoundError, ProviderError
from app.models.ai import AIModel, AIProviderRow
from app.services.ai.base import AIProvider
from app.services.ai.registry import get_provider

_TIER_RANK = {"free": 0, "plus": 1, "pro": 2}


@dataclass
class ResolvedModel:
    model: AIModel
    provider_row: AIProviderRow
    adapter: AIProvider


class AIRouter:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db

    async def resolve(
        self, model_code: str, *, user_tier: str = "free", region: str | None = None
    ) -> ResolvedModel:
        stmt = (
            select(AIModel)
            .options(selectinload(AIModel.provider))
            .where(AIModel.code == model_code, AIModel.deleted_at.is_(None))
        )
        model = (await self.db.execute(stmt)).scalar_one_or_none()
        if model is None:
            raise NotFoundError(f"مدل '{model_code}' یافت نشد", error_code="model_not_found")
        if not model.is_enabled:
            raise ForbiddenError("این مدل در حال حاضر غیرفعال است", error_code="model_disabled")

        # Tier gate.
        if _TIER_RANK.get(user_tier, 0) < _TIER_RANK.get(model.min_tier, 0):
            raise ForbiddenError(
                "برای استفاده از این مدل باید اشتراک خود را ارتقا دهید",
                error_code="tier_required",
            )

        # Region gate.
        if region and model.region_rules:
            blocked = set(model.region_rules.get("block", []))
            if region in blocked:
                raise ForbiddenError(
                    "این مدل در منطقهٔ شما در دسترس نیست", error_code="region_blocked"
                )

        provider_row = model.provider
        if provider_row is None or not provider_row.is_enabled:
            raise ProviderError("ارائه‌دهنده در دسترس نیست", error_code="provider_disabled")

        adapter = get_provider(provider_row.key)
        if adapter is None:
            raise ProviderError(
                f"آداپتور ارائه‌دهنده '{provider_row.key}' ثبت نشده است",
                error_code="provider_not_registered",
            )

        return ResolvedModel(model=model, provider_row=provider_row, adapter=adapter)

    async def resolve_with_fallback(
        self, model_code: str, *, user_tier: str = "free", region: str | None = None
    ) -> list[ResolvedModel]:
        """Return the primary model followed by its available fallbacks."""
        primary = await self.resolve(model_code, user_tier=user_tier, region=region)
        chain = [primary]
        for code in primary.model.fallback_codes or []:
            try:
                chain.append(
                    await self.resolve(code, user_tier=user_tier, region=region)
                )
            except (NotFoundError, ForbiddenError, ProviderError):
                continue
        return chain
