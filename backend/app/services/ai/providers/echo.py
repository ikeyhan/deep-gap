"""Echo provider — a fully working mock so the whole chain runs without API keys.

It lets development, tests and demos exercise the AI Gateway, billing and chat
end-to-end. Replace/augment with real providers (OpenAI, Anthropic, ...) that
implement the same interface.
"""
from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator

from app.services.ai.base import (
    AIProvider,
    Capability,
    ImageRequest,
    ImageResponse,
    TextChunk,
    TextRequest,
    TextResponse,
    Usage,
)


def _estimate_tokens(text: str) -> int:
    # Rough heuristic; real providers return exact usage.
    return max(1, len(text) // 4)


class EchoProvider(AIProvider):
    key = "echo"
    capabilities = {Capability.TEXT, Capability.IMAGE}

    def _reply_text(self, req: TextRequest) -> str:
        last_user = next(
            (m.content for m in reversed(req.messages) if m.role == "user"),
            "",
        )
        return f"«دیپ گپ» (نمونه): {last_user}".strip()

    async def generate_text(self, req: TextRequest) -> TextResponse:
        reply = self._reply_text(req)
        input_tokens = sum(_estimate_tokens(m.content) for m in req.messages)
        return TextResponse(
            content=reply,
            usage=Usage(input_tokens=input_tokens, output_tokens=_estimate_tokens(reply)),
            model=req.model,
        )

    async def stream_text(self, req: TextRequest) -> AsyncIterator[TextChunk]:
        reply = self._reply_text(req)
        for word in reply.split(" "):
            await asyncio.sleep(0)  # cooperative yield
            yield TextChunk(delta=word + " ")
        yield TextChunk(delta="", finished=True)

    async def generate_image(self, req: ImageRequest) -> ImageResponse:
        placeholder = f"https://placehold.co/{req.size}?text=DeepGap"
        return ImageResponse(urls=[placeholder] * req.n, usage=Usage(output_tokens=1))
