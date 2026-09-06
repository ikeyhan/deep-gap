"""AI provider abstraction (spec rules 7, 8, 21, 102).

Every provider implements the same interface so new providers can be added
without touching the rest of the system. The Flutter app NEVER talks to a
provider directly and NEVER holds provider API keys — only this backend does.
"""
from __future__ import annotations

import abc
from collections.abc import AsyncIterator
from dataclasses import dataclass, field
from enum import Enum


class Capability(str, Enum):
    TEXT = "text"
    IMAGE = "image"
    VISION = "vision"
    SPEECH_TO_TEXT = "speech_to_text"
    TEXT_TO_SPEECH = "text_to_speech"
    EMBEDDING = "embedding"
    VIDEO = "video"
    MUSIC = "music"


@dataclass
class ChatMessage:
    role: str  # user | assistant | system
    content: str


@dataclass
class TextRequest:
    model: str  # provider-side model id
    messages: list[ChatMessage]
    max_tokens: int | None = None
    temperature: float = 0.7
    stream: bool = False
    request_id: str | None = None


@dataclass
class TextChunk:
    delta: str
    finished: bool = False


@dataclass
class Usage:
    input_tokens: int = 0
    output_tokens: int = 0


@dataclass
class TextResponse:
    content: str
    usage: Usage = field(default_factory=Usage)
    model: str = ""
    finish_reason: str = "stop"


@dataclass
class ImageRequest:
    model: str
    prompt: str
    size: str = "1024x1024"
    n: int = 1
    request_id: str | None = None


@dataclass
class ImageResponse:
    urls: list[str] = field(default_factory=list)
    usage: Usage = field(default_factory=Usage)


class AIProvider(abc.ABC):
    """Common interface for all AI providers.

    A concrete provider declares which capabilities it supports and implements
    the relevant methods. Unsupported methods raise NotImplementedError.
    """

    key: str = "base"
    capabilities: set[Capability] = set()

    def supports(self, capability: Capability) -> bool:
        return capability in self.capabilities

    async def generate_text(self, req: TextRequest) -> TextResponse:
        raise NotImplementedError

    async def stream_text(self, req: TextRequest) -> AsyncIterator[TextChunk]:
        raise NotImplementedError
        yield  # pragma: no cover  (makes this an async generator)

    async def generate_image(self, req: ImageRequest) -> ImageResponse:
        raise NotImplementedError

    async def health_check(self) -> bool:
        return True
