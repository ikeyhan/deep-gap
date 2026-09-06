"""Admin auth/RBAC unit tests (spec rules 77, 78)."""
import pytest

from app.core.deps import require_roles
from app.core.exceptions import ForbiddenError
from app.core.security import create_access_token, decode_token
from app.models.platform import AdminUser


def test_admin_token_carries_scope_and_role():
    token = create_access_token("admin-1", extra={"scope": "admin", "role": "finance"})
    payload = decode_token(token)
    assert payload["scope"] == "admin"
    assert payload["role"] == "finance"


@pytest.mark.asyncio
async def test_super_admin_passes_any_guard():
    guard = require_roles("finance")
    admin = AdminUser(role="super_admin")
    assert await guard(admin) is admin


@pytest.mark.asyncio
async def test_matching_role_passes():
    guard = require_roles("finance", "analyst")
    admin = AdminUser(role="analyst")
    assert await guard(admin) is admin


@pytest.mark.asyncio
async def test_wrong_role_is_forbidden():
    guard = require_roles("finance")
    admin = AdminUser(role="support")
    with pytest.raises(ForbiddenError):
        await guard(admin)
