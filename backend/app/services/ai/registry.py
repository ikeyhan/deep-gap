"""Provider registry — maps a provider key to a live adapter instance.

Adding a provider = implement AIProvider + register it here. No other part of
the system needs to change (spec rule 8).
"""
from __future__ import annotations

from app.services.ai.base import AIProvider
from app.services.ai.providers.echo import EchoProvider
from app.services.ai.providers.openrouter import build_from_settings as build_openrouter

_REGISTRY: dict[str, AIProvider] = {}


def register(provider: AIProvider) -> None:
    _REGISTRY[provider.key] = provider


def get_provider(key: str) -> AIProvider | None:
    return _REGISTRY.get(key)


def all_providers() -> dict[str, AIProvider]:
    return dict(_REGISTRY)


def bootstrap_providers() -> None:
    """Register built-in providers. Called once at app startup."""
    register(EchoProvider())
    # Real providers are registered only when their API key is configured.
    openrouter = build_openrouter()
    if openrouter:
        register(openrouter)


# Register defaults at import time so tests and workers have them too.
bootstrap_providers()
