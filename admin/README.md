# دیپ گپ — Admin Panel (Next.js + TypeScript)

پنل مدیریت با احراز هویت **مجزا** از کاربران، RBAC و Audit Log (spec §77-78).

## وضعیت
اسکلت App Router با layout راست‌به‌چپ، ناوبری، صفحهٔ داشبورد KPI و helper اتصال به API (`src/lib/api.ts`). صفحات مدیریت مدل/قیمت، کاربران، مالی و تنظیمات در فاز ۱۰ به endpointهای Admin وصل می‌شوند.

## اجرا
```bash
cd admin
npm install
NEXT_PUBLIC_API_URL=http://localhost:8000 npm run dev
# http://localhost:3000
```

## صفحات هدف
- `/` داشبورد (Users, Revenue, AI Cost, Gross Profit, Conversion, ...).
- `/models` ساخت/ویرایش/غیرفعال‌سازی مدل، قیمت، Provider، Fallback، ترتیب نمایش — بدون انتشار اپ.
- `/users` جستجو، مشاهده، بلاک، تعدیل اعتبار، تراکنش‌ها، اشتراک، دستگاه‌ها.
- `/finance` درآمد روزانه/۷/۳۰ روزه، Margin، سودده‌ترین مدل‌ها.
- `/config` Remote Config + Feature Flags.

## نقش‌ها
super_admin · support · finance · content · analyst — هر کدام Permission مجزا. تمام تغییرات مالی/مدل در `audit_logs` ثبت می‌شود.
