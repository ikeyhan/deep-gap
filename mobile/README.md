# دیپ گپ — Mobile (Flutter)

اپ اندروید دیپ گپ: **مینیمال، مشکی‌وسفید، Persian-first (RTL)** با انیمیشن در همهٔ بخش‌ها.

## وضعیت
صفحات اصلی MVP پیاده‌سازی و به Backend وصل شده‌اند:
- **Splash** با انیمیشن لوگو
- **Onboarding → ورود با موبایل → کد OTP** (انتقال‌های متحرک)
- **Home**: انتخاب دستیار (سریع/هوشمند/پژوهشگر/طراح) با انیمیشن انتخاب، نمایش اعتبار
- **Chat**: پیام‌های متحرک، نشانگر «در حال تایپ»، Markdown و RTL
- **Wallet/Paywall**: موجودی، پلن‌ها و بسته‌های اعتبار، شروع پرداخت

## طراحی
- **تم:** کاملاً مشکی‌وسفید و مینیمال. دارک = مشکی/سفید، **لایت = سفید/مشکی (برعکس)**.
  تغییر تم با دکمهٔ خورشید در بالای صفحه.
- **فونت:** `YekanBakh` (اصلی) + `Yekan` (جایگزین) — نگاه کنید به
  `assets/fonts/README.md` برای فعال‌سازی.
- **لوگو و آیکون‌ها:** همه **SVG** و theme-aware (با `flutter_svg` و رنگ‌آمیزی
  خودکار مطابق تم). لوگو در `assets/logo/deepgap_logo.svg`.
- **انیمیشن:** با `flutter_animate` (ظاهر شدن، جابه‌جایی، مقیاس) و انتقال‌های
  سفارشی صفحه (fade + slide) در `core/router.dart`.

## ساختار
```
lib/
  core/
    api_client.dart   اتصال JWT به همهٔ endpointها (+refresh خودکار)
    app_state.dart    وضعیت تم + نشست (Provider/ChangeNotifier)
    theme.dart        design system مشکی‌وسفید (لایت/دارک)
    tokens.dart       فاصله‌ها، شعاع‌ها، مدت انیمیشن، اندازهٔ متن‌ها
    icons.dart        آیکون‌های SVG + ویجت AppIcon و AppLogo
    router.dart       انتقال صفحهٔ متحرک (pushFade)
  features/
    splash/  onboarding+auth/  home/  chat/  wallet/
  app.dart            MaterialApp + RootGate (splash/auth/home)
  main.dart           راه‌اندازی + Provider
```

## اجرا
```bash
cd mobile
flutter pub get
# امولاتور اندروید به بک‌اند لوکال از طریق 10.0.2.2 وصل می‌شود
flutter run --dart-define=BASE_URL=http://10.0.2.2:8000
```
> توکن‌ها در `flutter_secure_storage` ذخیره و خودکار refresh می‌شوند. هیچ کلید
> Providerی در اپ نیست؛ فقط با Backend خودمان ارتباط دارد.

## بستهٔ فنی
provider · dio · flutter_secure_storage · flutter_svg · flutter_animate ·
flutter_markdown · intl.
