"""OpenRouter adapter tests using a mocked HTTP transport (no network)."""
import httpx
import pytest

from app.services.ai.base import Capability, ChatMessage, TextRequest
from app.services.ai.providers.openrouter import OpenRouterProvider


def _provider_with_handler(handler) -> OpenRouterProvider:
    provider = OpenRouterProvider(api_key="test-key")
    # Patch httpx.AsyncClient to use a mock transport.
    transport = httpx.MockTransport(handler)
    orig_init = httpx.AsyncClient.__init__

    def patched_init(self, *args, **kwargs):
        kwargs["transport"] = transport
        orig_init(self, *args, **kwargs)

    httpx.AsyncClient.__init__ = patched_init  # type: ignore[method-assign]
    provider._restore = lambda: setattr(httpx.AsyncClient, "__init__", orig_init)  # type: ignore[attr-defined]
    return provider


@pytest.mark.asyncio
async def test_generate_text_parses_completion():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == "Bearer test-key"
        body = request.read().decode()
        assert "gpt-4o-mini" in body
        return httpx.Response(
            200,
            json={
                "model": "openai/gpt-4o-mini",
                "choices": [
                    {"message": {"role": "assistant", "content": "سلام!"}, "finish_reason": "stop"}
                ],
                "usage": {"prompt_tokens": 12, "completion_tokens": 3},
            },
        )

    provider = _provider_with_handler(handler)
    try:
        resp = await provider.generate_text(
            TextRequest(model="openai/gpt-4o-mini", messages=[ChatMessage("user", "سلام")])
        )
        assert resp.content == "سلام!"
        assert resp.usage.input_tokens == 12
        assert resp.usage.output_tokens == 3
    finally:
        provider._restore()  # type: ignore[attr-defined]


@pytest.mark.asyncio
async def test_generate_text_raises_on_error():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(429, text="rate limited")

    provider = _provider_with_handler(handler)
    from app.core.exceptions import ProviderError

    try:
        with pytest.raises(ProviderError):
            await provider.generate_text(
                TextRequest(model="x", messages=[ChatMessage("user", "hi")])
            )
    finally:
        provider._restore()  # type: ignore[attr-defined]


def test_capabilities():
    p = OpenRouterProvider(api_key="k")
    assert p.supports(Capability.TEXT)


def test_parse_completion_handles_empty():
    resp = OpenRouterProvider._parse_completion({})
    assert resp.content == ""
    assert resp.usage.input_tokens == 0
