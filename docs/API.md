# Deep Gap API — v1 Reference

Base URL (dev): `http://localhost:8000/api/v1` — interactive docs at `/docs`.

All error responses use the standard envelope:
```json
{
  "error_code": "insufficient_credit",
  "message": "اعتبار کافی نیست",
  "details": null,
  "request_id": "…",
  "timestamp": "2026-01-01T00:00:00Z"
}
```
Authenticated endpoints require `Authorization: Bearer <access_token>`.

---

## Auth
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/otp/request` | `{phone}` | Rate-limited; returns `dev_code` in non-prod |
| POST | `/auth/otp/verify` | `{phone, code, device_id?}` | Returns token pair + `is_new_user` |
| POST | `/auth/refresh` | `{refresh_token}` | Rotates the refresh token |

**Token response**
```json
{"access_token":"…","refresh_token":"…","token_type":"bearer","expires_in":1800,"is_new_user":true}
```

## Users
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/users/me` | ✓ | Profile + active tier |
| DELETE | `/users/me` | ✓ | Soft-delete (privacy by design) |

## Models
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/models` | ✓ | Enabled models, ordered for Home (persona codes) |

## Conversations & Chat
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/conversations` | ✓ | Create conversation |
| GET | `/conversations` | ✓ | List (pinned first) |
| GET | `/conversations/{id}` | ✓ | Get one |
| DELETE | `/conversations/{id}` | ✓ | Soft-delete |
| GET | `/conversations/{id}/messages` | ✓ | Message history |
| POST | `/conversations/{id}/messages` | ✓ | Send + get AI reply (charges credit) |
| POST | `/conversations/{id}/messages/stream` | ✓ | SSE streaming; billing settles at end |

**Send message body:** `{content, model_code="fast", stream=false}`
**Response:** user_message, assistant_message, `charged_credit`, `balance`.

## Wallet
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/wallet` | ✓ | Balance breakdown |
| GET | `/wallet/transactions` | ✓ | Ledger (paginated) |

## Subscriptions & Credit Packages
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/subscriptions/plans` | — | Active plans (free/plus/pro) |
| GET | `/subscriptions/packages` | — | Active credit packages |
| GET | `/subscriptions/me` | ✓ | Current active subscription (or null) |

## Payments (server-side verification)
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/payments/initiate` | ✓ | `{product_type, code}` → payment + `payment_url`; price is server-set |
| GET/POST | `/payments/callback` | — | Gateway redirect target; verifies + fulfills (idempotent) |
| POST | `/payments/{id}/verify` | ✓ | Server-side receipt/IAP verification or re-check |
| GET | `/payments/{id}` | ✓ | Payment status |

**Flow:** `initiate` (creates `Payment`, price from DB) → user pays at gateway →
`callback`/`verify` recomputes proof **server-side** → on success, credit/subscription
is granted through the wallet ledger with a payment-scoped idempotency key (no
double-grant on repeated callbacks). A client-reported success is never trusted.

## Admin (separate auth + RBAC + audit)
Admin tokens carry `scope=admin` and a `role`; they are NOT interchangeable with
user tokens. Roles: `super_admin` (all), `finance`, `support`, `content`, `analyst`.
| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/admin/auth/login` | — | `{email, password}` → admin token |
| GET | `/admin/dashboard` | finance/analyst | KPIs (users, revenue, AI cost, gross profit, conversion) |
| GET | `/admin/finance/top-models` | finance/analyst | Most-used / most-profitable models |
| GET | `/admin/models` | any admin | List models |
| POST | `/admin/models` | content | Create model + pricing |
| PATCH | `/admin/models/{id}` | content | Update model and/or pricing |
| POST | `/admin/models/{id}/toggle?enabled=` | content | Enable/disable without an app release |
| GET | `/admin/users?q=` | support | Search users |
| POST | `/admin/users/{id}/block?blocked=` | support | Block/unblock |
| POST | `/admin/users/{id}/adjust-credit` | finance | `{amount, reason}` — ledgered + audited |

Every model change, block and credit adjustment writes an `audit_logs` row.

## Health
| Method | Path | Notes |
|---|---|---|
| GET | `/health` | Liveness |
| GET | `/health/ready` | DB + Redis readiness |

---

### Error codes (selection)
`unauthorized` · `otp_invalid` · `otp_expired` · `otp_too_many_attempts` ·
`rate_limited` · `insufficient_credit` · `model_not_found` · `model_disabled` ·
`tier_required` · `region_blocked` · `provider_error` · `pricing_missing` ·
`conversation_not_found` · `validation_error` · `internal_error`.
