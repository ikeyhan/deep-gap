"""AI gateway / provider abstraction tests."""
import pytest

from app.services.ai.base import Capability, ChatMessage, TextRequest
from app.services.ai.providers.echo import EchoProvider
from app.services.ai.registry import get_provider


@pytest.mark.asyncio
async def test_echo_generate_text():
    provider = EchoProvider()
    req = TextRequest(model="echo-fast", messages=[ChatMessage(role="user", content="سلام")])
    resp = await provider.generate_text(req)
    assert "سلام" in resp.content
    assert resp.usage.output_tokens >= 1


@pytest.mark.asyncio
async def test_echo_stream_text():
    provider = EchoProvider()
    req = TextRequest(model="echo-fast", messages=[ChatMessage(role="user", content="hi there")])
    chunks = [c async for c in provider.stream_text(req)]
    assert any(c.finished for c in chunks)
    assert "".join(c.delta for c in chunks).strip()


def test_registry_has_echo():
    assert get_provider("echo") is not None


def test_capabilities():
    provider = EchoProvider()
    assert provider.supports(Capability.TEXT)
    assert not provider.supports(Capability.VIDEO)
