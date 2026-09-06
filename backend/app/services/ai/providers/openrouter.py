"""OpenRouter provider — real multi-model text generation.

OpenRouter exposes an OpenAI-compatible Chat Completions API, so one adapter and
one API key give access to many models (OpenAI, Anthropic, Google, Llama, ...).
The key lives only on the server (spec rule 102); the app never sees it.
"""
from __future__ import annotations

import json
from collections.abc import AsyncIterator

import httpx

from app.core.config import settings
from app.core.exceptions import ProviderError
from app.services.ai.base import (
    AIProvider,
    Capability,
    TextChunk,
    TextRequest,
    TextResponse,
    Usage,
)


class OpenRouterProvider(AIProvider):
    key = "openrouter"
    capabilities = {Capability.TEXT, Capability.VISION}

    def __init__(self, api_key: str, base_url: str | None = None) -> None:
        self.api_key = api_key
        self.base_url = (base_url or settings.openrouter_base_url).rstrip("/")

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            # OpenRouter attribution headers (optional but recommended).
            "HTTP-Referer": settings.openrouter_site_url,
            "X-Title": settings.openrouter_app_name,
        }

    def _payload(self, req: TextRequest, *, stream: bool) -> dict:
        return {
            "model": req.model,
            "messages": [{"role": m.role, "content": m.content} for m in req.messages],
            "temperature": req.temperature,
            "max_tokens": req.max_tokens,
            "stream": stream,
        }

    @staticmethod
    def _parse_completion(data: dict) -> TextResponse:
        choice = (data.get("choices") or [{}])[0]
        message = choice.get("message") or {}
        usage = data.get("usage") or {}
        return TextResponse(
            content=message.get("content") or "",
            usage=Usage(
                input_tokens=int(usage.get("prompt_tokens") or 0),
                output_tokens=int(usage.get("completion_tokens") or 0),
            ),
            model=data.get("model") or "",
            finish_reason=choice.get("finish_reason") or "stop",
        )

    async def generate_text(self, req: TextRequest) -> TextResponse:
        async with httpx.AsyncClient(timeout=120) as client:
            resp = await client.post(
                f"{self.base_url}/chat/completions",
                headers=self._headers(),
                json=self._payload(req, stream=False),
            )
        if resp.status_code >= 400:
            raise ProviderError(
                f"OpenRouter error {resp.status_code}", details=resp.text[:500]
            )
        return self._parse_completion(resp.json())

    async def stream_text(self, req: TextRequest) -> AsyncIterator[TextChunk]:
        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream(
                "POST",
                f"{self.base_url}/chat/completions",
                headers=self._headers(),
                json=self._payload(req, stream=True),
            ) as resp:
                if resp.status_code >= 400:
                    body = await resp.aread()
                    raise ProviderError(
                        f"OpenRouter error {resp.status_code}",
                        details=body[:500].decode(errors="ignore"),
                    )
                async for line in resp.aiter_lines():
                    if not line or not line.startswith("data:"):
                        continue
                    data = line[len("data:") :].strip()
                    if data == "[DONE]":
                        break
                    try:
                        chunk = json.loads(data)
                    except json.JSONDecodeError:
                        continue
                    delta = (chunk.get("choices") or [{}])[0].get("delta") or {}
                    piece = delta.get("content")
                    if piece:
                        yield TextChunk(delta=piece)
        yield TextChunk(delta="", finished=True)

    async def health_check(self) -> bool:
        return bool(self.api_key)


def build_from_settings() -> OpenRouterProvider | None:
    if not settings.openrouter_api_key:
        return None
    return OpenRouterProvider(settings.openrouter_api_key)
