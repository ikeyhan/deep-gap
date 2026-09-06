"""End-to-end integration test against a live PostgreSQL + Redis.

Skipped unless RUN_INTEGRATION=1 and a real database is configured. In CI this
runs with service containers; locally, point DATABASE_URL/REDIS_URL at a running
Postgres/Redis and run `alembic upgrade head && python -m app.seed` first.

Covers: OTP auth + signup bonus → chat billing → refresh rotation → payment
initiate + server-side verify → idempotent re-callback (no double-credit) →
subscription upgrade + tier gate → admin dashboard.
"""
from __future__ import annotations

import os
from urllib.parse import parse_qs, urlparse

import pytest

pytestmark = pytest.mark.skipif(
    os.getenv("RUN_INTEGRATION") != "1",
    reason="integration test — set RUN_INTEGRATION=1 with a live DB",
)


@pytest.mark.asyncio
async def test_full_flow():
    import random

    import httpx

    from app.main import app

    P = "/api/v1"
    transport = httpx.ASGITransport(app=app)
    async with app.router.lifespan_context(app):
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as c:
            # Unique phone per run so the test is rerunnable on a persistent DB.
            phone = f"0912{random.randint(1000000, 9999999)}"
            code = (await c.post(f"{P}/auth/otp/request", json={"phone": phone})).json()["dev_code"]
            tok = (await c.post(f"{P}/auth/otp/verify", json={"phone": phone, "code": code})).json()
            h = {"Authorization": f"Bearer {tok['access_token']}"}
            assert tok["is_new_user"] is True

            # Signup bonus present.
            bal = float((await c.get(f"{P}/wallet", headers=h)).json()["balance"])
            assert bal >= 100

            # Chat charges credit.
            conv_resp = await c.post(f"{P}/conversations", headers=h, json={"model_code": "fast"})
            conv = conv_resp.json()["id"]
            msg = (
                await c.post(
                    f"{P}/conversations/{conv}/messages",
                    headers=h,
                    json={"content": "سلام", "model_code": "fast"},
                )
            ).json()
            assert msg["charged_credit"] > 0
            assert msg["balance"] < bal

            # Refresh rotation issues a new token.
            r = await c.post(f"{P}/auth/refresh", json={"refresh_token": tok["refresh_token"]})
            assert "access_token" in r.json()

            # Buy a credit package via server-verified payment.
            pay = (
                await c.post(
                    f"{P}/payments/initiate",
                    headers=h,
                    json={"product_type": "credit_package", "code": "standard"},
                )
            ).json()
            q = parse_qs(urlparse(pay["payment_url"]).query)
            cb = {
                "payment_id": q["payment_id"][0],
                "provider_ref": q["provider_ref"][0],
                "status": q["status"][0],
                "signature": q["signature"][0],
            }
            assert (await c.get(f"{P}/payments/callback", params=cb)).json()["status"] == "verified"
            after = float((await c.get(f"{P}/wallet", headers=h)).json()["balance"])

            # Idempotent re-callback must not double-credit.
            await c.get(f"{P}/payments/callback", params=cb)
            after2 = float((await c.get(f"{P}/wallet", headers=h)).json()["balance"])
            assert after == after2

            # Tier gate: researcher (pro) is blocked before upgrade.
            r = await c.post(f"{P}/conversations", headers=h, json={"model_code": "researcher"})
            rid = r.json()["id"]
            blocked = await c.post(
                f"{P}/conversations/{rid}/messages",
                headers=h,
                json={"content": "x", "model_code": "researcher"},
            )
            assert blocked.status_code == 403

            # Upgrade to Pro.
            pro = (
                await c.post(
                    f"{P}/payments/initiate",
                    headers=h,
                    json={"product_type": "subscription", "code": "pro"},
                )
            ).json()
            q2 = parse_qs(urlparse(pro["payment_url"]).query)
            await c.get(
                f"{P}/payments/callback",
                params={
                    "payment_id": q2["payment_id"][0],
                    "provider_ref": q2["provider_ref"][0],
                    "status": q2["status"][0],
                    "signature": q2["signature"][0],
                },
            )
            assert (await c.get(f"{P}/users/me", headers=h)).json()["tier"] == "pro"

            # Now researcher works.
            ok = await c.post(
                f"{P}/conversations/{rid}/messages",
                headers=h,
                json={"content": "تحلیل", "model_code": "researcher"},
            )
            assert ok.status_code == 200

            # Admin dashboard.
            atok = (
                await c.post(
                    f"{P}/admin/auth/login",
                    json={"email": "admin@deepgap.local", "password": "change-me-admin"},
                )
            ).json()
            ah = {"Authorization": f"Bearer {atok['access_token']}"}
            dash = (await c.get(f"{P}/admin/dashboard", headers=ah)).json()
            assert dash["paid_users"] >= 1
            assert dash["total_users"] >= 1
