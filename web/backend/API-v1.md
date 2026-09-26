# API v1: mobile app and site sync

Base path: `/api/v1`. It runs on the same Express server and SQLite database as the website, with the same user accounts.
The legacy routes (`/api/*`) are unchanged and the website keeps working exactly as before.

## Conventions

| Topic | Rule |
|---|---|
| Format | JSON. Request body limit 256 KB |
| Errors | `{ "error": { "code": "machine_code", "message": "user-facing Persian text", "fields": { "phone": "…" } } }` |
| Paging | `?page=1&limit=20` (max 50) → `{ items, page, limit, total, hasMore }` |
| Auth | `Authorization: Bearer <accessToken>` |
| Cache | Public data sends `Cache-Control: public, max-age=…` plus an ETag (304 on revalidation). Personal data sends `private, no-store` |
| Rate limits | 300 req/min per IP overall. Login/registration 10 per 15 min. Orders 20 per 10 min. Reviews, messages and tracking have their own limits |
| Maintenance | When the panel setting `maintenance=1` is on, everything except `/config` returns `503 maintenance` |
| Version | Every response carries the `X-API-Version: 1` header |

### Common error codes
`invalid_input` (with `fields`), `auth_required`, `token_invalid`, `account_inactive`, `staff_not_allowed`,
`invalid_credentials`, `account_locked`, `rate_limited`, `not_found`, `username_taken`, `phone_taken`,
`out_of_stock`, `product_unavailable`, `coupon_invalid`, `min_order`, `idempotency_key_required`,
`maintenance`, `server_error`.

## Authentication and roles

- **Access token:** a 15-minute JWT (`APP_ACCESS_TTL_SEC`).
- **Refresh token:** random, 30 days (`APP_REFRESH_TTL_DAYS`). Only its SHA-256 hash is stored. Every refresh rotates it. Presenting an already-revoked token revokes the whole chain (theft detection).
- **Allowed roles in the app:** `customer`, `seller` and `office`. Admin panel staff (`admin`, `editor`, `support`) get `403 staff_not_allowed`, both at login and with an existing token. The admin panel is fully separate, and no admin capability exists in this API.
- **Password change** (site or app) revokes every refresh token.

| Method | Path | Description |
|---|---|---|
| POST | `/auth/login` | `{username, password}`. Username or mobile number → `{accessToken, refreshToken, expiresIn, user}` |
| POST | `/auth/register` | Customer sign-up `{name, phone, username, password(≥8), email?, city?}` |
| POST | `/auth/refresh` | `{refreshToken}` → new token pair |
| POST | `/auth/logout` | `{refreshToken, deviceToken?}` |
| POST | `/auth/change-password` | `{current, next}` → a fresh token pair for this device |

## Public data

| Method | Path | Description |
|---|---|---|
| GET | `/config` | Site name, contact details, shipping cost, support, maintenance, minimum app version |
| GET | `/home` | Slides, categories, newest/popular, sellers, offices, articles in one request |
| GET | `/categories` | Product categories with counts |
| GET | `/products` | `q, category, sellerId, minPrice, maxPrice, inStock=1, sort=newest\|price_asc\|price_desc\|popular\|rating` |
| GET | `/products/:id` | Details, approved reviews, rating distribution, related products |
| POST | `/products/:id/reviews` | Login required. Shown after admin approval |
| GET | `/sellers`, `/sellers/:id` | Stores |
| GET | `/offices`, `/offices/:id` | Verified offices and their services |
| POST | `/offices/:id/messages` | Message an office (optional login. When logged in, the reply shows in the account) |
| GET | `/articles`, `/articles/:id` | Blog |
| GET | `/faqs` | Frequently asked questions |
| GET | `/track?code&phone` | Guest tracking (no address returned) |

## Cart and orders

- `POST /cart/quote` takes `{items:[{productId, qty}], coupon?}`. The server builds the invoice from database prices. **Client-sent prices are ignored.**
- `POST /orders` requires an `Idempotency-Key` header (8–100 chars). A resend with the same key returns the same response (`Idempotent-Replay: true`) and never creates a duplicate order. Login is optional (guest checkout, as on the website).
- `GET /orders`, `GET /orders/:code`: the user's own orders, **by account ownership only**.
- `POST /orders/claim` takes `{code, phone}` and attaches a guest order to the account. It needs the same proof as tracking.
- `GET /my/products?status=active|delivered|returned`: the purchased items, each with its order status.

> **Payments:** the website has no connected payment gateway yet. Orders are recorded and payment is arranged afterwards, by phone or on delivery.
> The API reports this as `commerce.onlinePayment=false`. When a gateway is added, payment verification must happen only on the
> server (the gateway callback), with an idempotency check on the transaction id.

## Account

| Method | Path | Description |
|---|---|---|
| GET/PUT | `/me` | Profile and counters (unread notifications, orders, wishlist) |
| DELETE | `/me` | Account deletion with `{password}` (Google Play requirement). Personal data is erased. Order rows stay for accounting |
| GET | `/wishlist` | Wishlist shared by site and app |
| PUT/DELETE | `/wishlist/:productId` | Add or remove |
| POST | `/wishlist/merge` | `{productIds}` merges a local list |
| GET | `/notifications?since=<id>` | Notifications (incremental sync) plus `unread` |
| POST | `/notifications/read` | `{ids}` or `{all:true}` |
| POST | `/devices` | Registers a device for push (`provider: fcm\|none`) |
| GET/POST | `/support/messages` | The user's support tickets and the support team's reply |
| GET | `/support/office-messages` | The user's conversations with offices, with each office's reply |

## Notifications

`src/notify.js` records notifications on these events: order placed, order status change (from the admin panel or the seller dashboard), support reply, and office reply.
The app currently fetches them through periodic sync (WorkManager). The `devices` table is ready for server-side FCM sending.

## Tests

```bash
cd backend && npm test   # starts a real server against a temporary database
```
