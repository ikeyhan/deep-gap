# دیپ گپ — Deep Gap

پلتفرم هوش مصنوعی فارسی، **چندمدلی** و **درآمدزا** برای Android.
یک AI Super App که چند سرویس هوش مصنوعی (چت، تصویر، فایل، صوت، ...) را از پشت یک **AI Gateway** واحد و امن ارائه می‌دهد — بدون قرار دادن هیچ کلید Providerی داخل اپ.

> وضعیت فعلی: **Phase 0 (طراحی) + بخش عمدهٔ هستهٔ Backend (Phase 1/2/4/5/6)** پیاده‌سازی و تست شده است. نقشهٔ کامل و تصمیم‌های معماری در [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## معماری در یک نگاه
```
Flutter App ──HTTPS(JWT)──> FastAPI Backend ──> AI Gateway ──> Provider Adapter ──> AI Provider
                                   │
                          Billing · Wallet Ledger · PostgreSQL · Redis · S3
```
- **Backend:** FastAPI + SQLAlchemy(async) + PostgreSQL + Redis — Modular Monolith.
- **AI Gateway:** Provider abstraction + registry + smart router + fallback. یک provider نمونهٔ کاملاً کارکردی (`echo`) بدون نیاز به کلید API.
- **Billing:** اعتبار سمت سرور، Ledger تراکنشی با قفل ردیفی و Idempotency، محاسبهٔ سود هر درخواست.
- **Mobile:** Flutter (Clean Architecture) — اسکلت در `mobile/`.
- **Admin:** Next.js + TypeScript — اسکلت در `admin/`.

## پیش‌نیازها
- Docker + Docker Compose (مسیر ساده)، یا
- Python 3.12+، PostgreSQL 16، Redis 7 (اجرای دستی).

## اجرای سریع (Docker)
```bash
cp .env.example .env          # مقادیر را در صورت نیاز ویرایش کنید
docker compose up --build     # postgres, redis, minio, backend, nginx
# migrations به‌صورت خودکار اجرا می‌شوند (alembic upgrade head)
docker compose exec backend python -m app.seed   # دادهٔ اولیه (مدل‌ها، پلن‌ها، ...)
```
- API: http://localhost:8000  ·  Docs: http://localhost:8000/docs
- از پشت Nginx: http://localhost:8080

## اجرای دستی Backend (dev)
```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export DATABASE_URL=postgresql+asyncpg://deepgap:deepgap_dev_password@localhost:5432/deepgap
export REDIS_URL=redis://localhost:6379/0
alembic upgrade head
python -m app.seed
uvicorn app.main:app --reload
```

## تست و لینت
```bash
cd backend
pytest -q          # ۲۲ تست واحد (billing, JWT/auth, gateway, payments, admin RBAC)
ruff check app     # لینت

# تست یکپارچهٔ end-to-end روی دیتابیس واقعی (نیازمند Postgres + Redis):
export DATABASE_URL=... REDIS_URL=... RUN_INTEGRATION=1
alembic upgrade head && python -m app.seed
pytest tests/test_integration_flow.py   # OTP→چت→پرداخت→اشتراک→ادمین
```
> جریان کامل (احراز هویت، صورت‌حساب، پرداخت با تأیید سرور و ضدِ double-credit،
> ارتقای اشتراک و گیت tier، و داشبورد ادمین) روی Postgres واقعی تست شده است.
> ادمین پیش‌فرض seed: `admin@deepgap.local` / `change-me-admin` (حتماً در production تغییر دهید).

## جریان یک درخواست چت (خلاصه)
1. `POST /auth/otp/request` → دریافت کد (در dev کد در پاسخ `dev_code` است).
2. `POST /auth/otp/verify` → توکن Access/Refresh.
3. `POST /conversations` → ساخت گفت‌وگو.
4. `POST /conversations/{id}/messages` با `{"content":"سلام","model_code":"fast"}`.
   - Preflight اعتبار → فراخوانی مدل → کسر اعتبار → ثبت UsageLog → پاسخ.
5. `GET /wallet` → مشاهدهٔ موجودی و کسر.

مدل‌های پیش‌فرض (persona-based): `fast` (سریع)، `smart` (هوشمند، Plus)، `researcher` (پژوهشگر، Pro)، `designer` (طراح/تصویر، Plus).

## اصول کلیدی (رعایت‌شده در کد)
- کلید Provider هرگز در اپ نیست؛ فقط Backend با Provider حرف می‌زند.
- Client قیمت/هزینه/موجودی را تعیین نمی‌کند — **Server منبع حقیقت است**.
- هر تغییر موجودی، یک ردیف Ledger با Idempotency و قفل ردیفی دارد.
- خطای Provider → پیام کاربر بدون کسر اعتبار (زمینهٔ Refund/Fallback آماده).
- داده‌های حساس (OTP، توکن، رمز، کلید) در لاگ redact می‌شوند.

## ساختار پروژه
```
deep-gap/
├── backend/      FastAPI app, Alembic, tests
├── mobile/       Flutter (Clean Architecture) — اسکلت
├── admin/        Next.js + TS — اسکلت
├── database/     schema.sql (مرجع، تولیدشده از مدل‌ها)
├── infrastructure/nginx, scripts
├── docs/         ARCHITECTURE.md, API.md
└── docker-compose.yml, .env.example
```

## نقشهٔ راه
- **V1 (MVP):** Auth/OTP، Chat + Streaming، چند مدل، Wallet/Credit، Subscription/Payment، Admin، Analytics پایه.
- **V2:** PDF/File، Web Search، TTS/Voice، Memory، Prompt Marketplace، Referral، Daily Reward.
- **V3:** Video، Music، Agents، Workspace، Team/Business، Developer API.

جزئیات فازها و Definition of Done در [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## امنیت و اسرار
`.env` هرگز commit نمی‌شود. نمونه در `.env.example`. کلیدهای Provider/Payment/FCM فقط سمت سرور نگهداری می‌شوند.
