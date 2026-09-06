# دیپ گپ (Deep Gap) — سند معماری و محصول (Phase 0)

> پلتفرم هوش مصنوعی فارسی، چندمدلی و درآمدزا برای Android.
> این سند مرجع تصمیم‌های محصول، معماری و فازبندی است.

---

## ۱. Executive Summary
دیپ گپ یک **AI Super App** فارسی است که چند سرویس هوش مصنوعی (چت، تصویر، فایل، صوت، جستجو و ...) را از پشت یک **AI Gateway** واحد به کاربر ارائه می‌دهد. کلیدهای Provider هرگز وارد اپ نمی‌شوند؛ اپ فقط با Backend خودمان حرف می‌زند. سیستم از روز اول بر پایهٔ **Credit + Subscription** و **کنترل دقیق هزینهٔ Provider** طراحی شده تا هر درخواست، Margin مثبت داشته باشد.

## ۲. Product Vision
تبدیل شدن به «دستیار هوش مصنوعی پیش‌فرض کاربر فارسی‌زبان» — جایی که کاربر بدون دانستن نام فنی مدل‌ها، متناسب با نیازش (سریع/هوشمند/پژوهشگر/طراح/دانشجو/کسب‌وکار) بهترین نتیجه را با شفاف‌ترین قیمت می‌گیرد.

## ۳. ارزش پیشنهادی اصلی (Value Proposition)
- **سادگی:** انتخاب بر اساس «کار» نه نام مدل.
- **چندمدلی:** بهترین مدل برای هر وظیفه، با Fallback خودکار.
- **فارسی-first:** RTL کامل، لحن فارسی، بازار پرداخت ایران.
- **شفافیت هزینه:** کاربر دقیقاً می‌داند هر کار چند اعتبار می‌برد.

## ۴. کاربران هدف
- دانشجویان و دانش‌آموزان (جزوه، آزمون، خلاصه).
- تولیدکنندگان محتوا و ادمین‌های اینستاگرام.
- کسب‌وکارهای کوچک (کپشن، تبلیغات، ایمیل).
- برنامه‌نویسان و کاربران عمومی روزمره.

## ۵. پنج مزیت رقابتی برای بازار فارسی
1. RTL و کیفیت زبان فارسی به‌عنوان شهروند درجه‌یک (نه ترجمهٔ ماشینی).
2. پرداخت بومی: درگاه ایرانی + پرداخت درون‌برنامه‌ای مارکت‌های داخلی.
3. مدل قیمت‌گذاری اعتبار شفاف و قابل کنترل از Admin بدون انتشار اپ.
4. مجموعهٔ دستیارها و Prompt Marketplace فارسی آماده.
5. کنترل هزینه و Smart Routing برای پایداری در برابر نوسان دسترسی Providerها.

## ۶. Feature Priority برای MVP (V1)
Splash · ورود موبایل + OTP · Home · AI Chat (چند مدل) · Streaming · History · ورودی تصویر · تولید تصویر · Speech-to-Text · Wallet · Credit · Subscription · Payment · Admin Panel · Usage Tracking · Analytics پایه.

## ۷. فعلاً نباید بسازیم
Video · Music · AI Agents پیشرفته · Workspace · Team/Business · Developer API · Voice Chat زندهٔ Interrupt-محور · iOS/Desktop. (به V2/V3 موکول می‌شوند.)

## ۸. معماری کامل سیستم
**Modular Monolith** روی FastAPI (نه Microservice زودهنگام) با مرزهای ماژولی تمیز تا بعداً بتوان Billing / AI Gateway / File Processing / Notification را جدا کرد.

- **Mobile:** Flutter (Clean Architecture: presentation / domain / data).
- **Backend:** FastAPI + SQLAlchemy async + PostgreSQL + Redis.
- **AI Gateway:** لایهٔ Provider Abstraction + Registry + Smart Router + Fallback.
- **Admin:** Next.js + TypeScript.
- **Storage:** S3-compatible (MinIO در dev).
- **Infra:** Docker Compose (dev)، Nginx، SSL (prod).

## ۹. دیاگرام جریان Client → Backend → AI Gateway → Provider
```
[Flutter App]
     │  HTTPS (JWT)         ← هیچ کلید Providerی در اپ نیست
     ▼
[Backend API  /api/v1]
     │  Auth · RateLimit · Validation
     ▼
[Billing Preflight]  ── اعتبار؟ پلن؟ محدودیت؟ قیمت مشخص؟
     │  (اگر رد شد → 402/403)
     ▼
[AI Gateway]
     ├─ Provider Router (نوع/کیفیت/قیمت/سرعت/tier/region)
     ▼
[Provider Adapter]  (echo | openai | anthropic | ...)
     │  در صورت خطا → Fallback Adapter
     ▼
[AI Provider API]
     │
     ▼
[Settle Billing]  ── ثبت UsageLog · کسر اعتبار · محاسبهٔ Gross Profit
     ▼
[Response / SSE Stream]  → App
```

## ۱۰. ساختار Repository
```
deep-gap/
  mobile/          Flutter app
  backend/         FastAPI (app/, alembic/, tests/)
  admin/           Next.js admin panel
  database/        schema.sql مرجع
  infrastructure/  nginx/, scripts/
  docker/          Dockerfileهای کمکی
  docs/            ARCHITECTURE.md, API.md
  docker-compose.yml · .env.example · README.md
```

## ۱۱. ماژول‌های Backend
`core` (config/db/redis/security/logging/exceptions/deps) · `models` · `schemas` · `api/v1` · `services` (auth, otp, wallet, billing, ai/{base,registry,router,providers}) · `alembic`.

## ۱۲. ساختار Flutter
```
lib/
  core/        (network, theme, di, router, storage)
  data/        (models, datasources, repositories impl)
  domain/      (entities, repositories, usecases)
  presentation/(features/{auth,home,chat,wallet,...}/{pages,widgets,bloc})
  l10n/        (fa, en)
```

## ۱۳. ساختار Admin Panel
Next.js App Router: `dashboard` · `users` · `models` (+pricing/provider/fallback) · `finance` · `subscriptions` · `config` (remote config + feature flags) · `audit`. احراز هویت مجزا، RBAC (super_admin/support/finance/content/analyst)، Audit Log.

## ۱۴. Database ERD (متنی)
```
users 1──1 user_profiles
users 1──* devices
users 1──* sessions
users 1──1 wallets 1──* wallet_transactions
users 1──* conversations 1──* messages 1──* message_attachments
users 1──* usage_logs *──1 ai_models *──1 ai_providers
ai_models 1──1 model_pricing
users 1──* subscriptions *──1 subscription_plans
users 1──* payments
credit_packages (مستقل)
users 1──* referrals (referrer/referred)
admin_users · audit_logs · feature_flags · remote_config
```

## ۱۵. لیست جداول
users, user_profiles, devices, sessions, wallets, wallet_transactions,
ai_providers, ai_models, model_pricing, usage_logs, conversations, messages,
message_attachments, subscription_plans, subscriptions, credit_packages,
payments, referrals, admin_users, audit_logs, feature_flags, remote_config.
(جداول V2/V3 مثل files, prompts, assistants, notifications, rewards در فازهای بعد.)

## ۱۶. API Endpoint Map (خلاصه)
جزئیات کامل در [`API.md`](./API.md).
`/auth/otp/request` · `/auth/otp/verify` · `/auth/refresh` · `/users/me` ·
`/models` · `/conversations` (CRUD) · `/conversations/{id}/messages` (+`/stream`) ·
`/wallet` · `/wallet/transactions` · `/health`.

## ۱۷. Authentication Flow
1. کاربر موبایل می‌دهد → `otp/request` (Redis + rate limit + cooldown).
2. `otp/verify` → کاربر ساخته/یافته می‌شود، bonus ثبت‌نام واریز، Session ساخته.
3. صدور **Access (کوتاه) + Refresh (jti در DB)**.
4. `refresh` → **Rotation**: نشست قدیمی revoke، جفت جدید صادر.
5. Logout → revoke نشست.

## ۱۸. AI Request Flow
Preflight (اعتبار/پلن/قیمت) → ثبت پیام کاربر → `open_usage(pending)` → فراخوانی Adapter (با Fallback) → موفق: `settle_usage` (کسر اعتبار + سود) / ناموفق: `fail_usage` (بدون کسر). در Streaming، Settle **بعد از پایان استریم** انجام می‌شود تا خروجی رایگان نشود.

## ۱۹. Billing Flow
`estimate_max_charge` برای گیت اعتبار → پس از پاسخ، `user_charge` بر اساس توکن‌های واقعی و `minimum_charge`. هر کسر/واریز از طریق `WalletService` با **قفل ردیفی (FOR UPDATE)** و **Idempotency Key** و ثبت **WalletTransaction** انجام می‌شود. هیچ‌جای دیگری balance را مستقیم تغییر نمی‌دهد.

## ۲۰. Payment Flow
`initiate` (ثبت Payment با idempotency_key) → هدایت به درگاه/مارکت → callback → **Server-Side Verification** → در صورت تأیید: واریز اعتبار/فعال‌سازی اشتراک از طریق Wallet Ledger. هیچ خریدی صرفاً با ادعای Client معتبر نیست.

## ۲۱. AI Provider Abstraction
`AIProvider` (base) با متدهای `generate_text/stream_text/generate_image/...` و مجموعهٔ `capabilities`. افزودن Provider = پیاده‌سازی interface + ثبت در `registry`. بقیهٔ سیستم تغییر نمی‌کند.

## ۲۲. مدل Wallet/Ledger
`wallets(balance, locked_balance, lifetime_credit, spent_credit, bonus_credit)` +
`wallet_transactions(type, amount, balance_after, reason, reference, idempotency_key)` — immutable و Auditable.

## ۲۳. مدل اشتراک
`subscription_plans(code=free|plus|pro, price, duration_days, monthly_credit, features JSONB)` + `subscriptions(status, started_at, expires_at, auto_renew)`. tier کاربر از اشتراک فعال محاسبه و گیت مدل‌ها با آن انجام می‌شود.

## ۲۴. مدل قیمت‌گذاری
`model_pricing(provider_cost_*, internal_cost, user_price_*, minimum_charge, minimum_profit)`. سود ناخالص هر درخواست = `charged_credit − provider_cost` و در `usage_logs.gross_profit` ثبت می‌شود.

## ۲۵. سیستم کنترل هزینهٔ AI
Preflight برآورد سقف هزینه · `minimum_charge` · `max_tokens` مدل · محدودیت‌های Daily/Monthly/Rate (Redis) · Feature Flag برای مدل‌های گران · قابلیت غیرفعال‌سازی مدل از Admin بدون انتشار اپ.

## ۲۶. مدل درآمدی پیشنهادی
Subscription (اصلی) + Credit Packs (مصرفی) + مدل‌های Premium + قابلیت‌های گران به‌صورت اعتبارمحور. تبلیغات فقط در Free و در صورت اقتصادی بودن.

## ۲۷. Free / Plus / Pro (پیشنهاد اولیه)
| | Free | Plus | Pro |
|---|---|---|---|
| پیام روزانه | ۲۰ | ۲۰۰ | ۱۰۰۰ |
| مدل‌ها | سریع | +هوشمند | +پژوهشگر |
| فایل/تصویر | محدود | بله | بیشتر |
| Web/Voice/Memory | خیر | خیر | بله |
| اعتبار ماهانه | ۱۰۰ | ۲۰۰۰ | ۶۰۰۰ |
(اعداد قابل تغییر از Admin/Remote Config.)

## ۲۸. Credit System
واحد داخلی «اعتبار». بسته‌ها: Starter/Standard/Power (+bonus). قیمت‌ها از Admin. Client هرگز قیمت/هزینه/موجودی را تعیین نمی‌کند — Server منبع حقیقت است.

## ۲۹. شاخص‌های Analytics
Event: app_open, signup_started/completed, chat_created, message_sent, image_generated, paywall_view, purchase_started/completed/failed, credit_low, subscription_cancelled. Metric: DAU/WAU/MAU, D1/D7/D30, ARPU/ARPPU, LTV, Conversion, Churn, Gross Margin.

## ۳۰. Security Checklist
کلید Provider فقط Server-side · Secrets در env و خارج از Git · JWT + Refresh Rotation · Rate Limit (auth/otp/chat/image/payment) · Input Validation · محافظت SQLi/XSS/CSRF · Secure Headers/CORS · Brute-force & OTP limit · Admin RBAC + Audit + 2FA-ready · Server-side Payment Verification · Ledger برای هر تغییر مالی.

## ۳۱. Infrastructure Plan
dev: Docker Compose (postgres/redis/minio/backend/nginx). prod: Ubuntu + Docker + Nginx + SSL، PostgreSQL با Backup خودکار + Retention، Redis persistent، Sentry + Structured Logging + Health Checks.

## ۳۲. Deployment Plan
Server setup → Firewall → Docker → env/secrets → `alembic upgrade head` → seed → Nginx + Domain + SSL → Backup + Monitoring → Deploy → Rollback (نگهداری نسخهٔ قبلی image + migration downgrade محتاطانه). سه محیط: development / staging / production.

## ۳۳. مراحل ساخت MVP
Phase 1 (Backend Foundation ✅) → 2 Auth ✅ → 3 Flutter Foundation → 4 AI Gateway ✅ → 5 Chat+Streaming ✅ → 6 Wallet/Billing ✅ → 7 Subscription/Payment ✅ → 8 Image/Vision → 9 Voice(STT) → 10 Admin API ✅ → 11 Analytics/Config → 12 Security/Abuse → 13 Test/Perf (unit + live integration ✅) → 14 Deploy → 15 Android Release → 16 Monitoring.

## ۳۴. ریسک‌های فنی
نوسان دسترسی Providerها (→ Fallback + Retry) · هزینهٔ Streaming (→ Settle بعد از استریم) · Race Condition مالی (→ FOR UPDATE + Idempotency) · RTL/Markdown پیچیده در Flutter · مقیاس فایل/Embedding (→ Queue در V2).

## ۳۵. ریسک‌های تجاری
Margin منفی روی مدل‌های گران (→ Unit Economics قبل از انتشار) · Abuse/Farming اعتبار (→ Anti-abuse) · Conversion پایین (→ Paywall ارزش‌محور + Trial) · تحریم/پرداخت (→ درگاه‌های بومی).

## ۳۶. کاهش هزینهٔ اولیه
شروع با Free Tier محدود و مدل اقتصادی · Cache پاسخ‌های تکراری · minimum_charge · محدودیت روزانه · مدل‌های گران پشت Feature Flag/Pro.

## ۳۷. افزایش Conversion
Trial + Starter Credits · First-purchase Offer · Paywall ارزش‌محور (نه Feature-list) · Annual Discount · Referral. بدون Dark Pattern.

## ۳۸. افزایش Retention
History/Favorites · Memory · Daily Reward/Streak · Saved Assistants · Prompt Library · Notification هدفمند (نه اسپم) · کارایی روزمرهٔ واقعی.

## ۳۹. افزایش LTV
ارتقای Free→Plus→Pro · Credit Packs مکرر · قابلیت‌های Premium (تصویر/صوت/فایل) · نگه‌داشت طولانی از طریق Memory و Workspace (V3).

## ۴۰. Definition of Done برای MVP
هر Feature: Backend + Validation + Error Handling + Security + Database + Test حداقلی + Integration موبایل (در صورت نیاز) + Documentation. MVP وقتی Done است که کاربر بتواند: ثبت‌نام با OTP → چت با چند مدل به‌صورت Streaming → دیدن اعتبار → خرید اشتراک/اعتبار با تأیید سمت سرور → و Admin بتواند مدل/قیمت/کاربر را مدیریت کند.
