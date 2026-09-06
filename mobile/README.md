# دیپ گپ — Mobile (Flutter)

اپ اندروید دیپ گپ با **Clean Architecture** و **Persian-first / RTL**.

## وضعیت
اسکلت پایه: تم مستقل برند، پیکربندی RTL/Localization، و `ApiClient` کامل (JWT + refresh rotation خودکار) که به تمام endpointهای Backend وصل است. صفحات feature در فازهای بعد ساخته می‌شوند.

## ساختار هدف
```
lib/
  core/            api_client.dart · theme.dart · di · router · storage
  data/            models · datasources · repositories (impl)
  domain/          entities · repositories (abstract) · usecases
  presentation/
    features/
      auth/        (phone + OTP)
      home/        (personas, credit, subscription)
      chat/        (streaming, markdown/RTL, history)
      wallet/      (balance, packages)
      subscription/(paywall)
  l10n/            fa, en
```

## اجرا
```bash
cd mobile
flutter pub get
# امولاتور اندروید به بک‌اند لوکال از طریق 10.0.2.2 وصل می‌شود
flutter run --dart-define=BASE_URL=http://10.0.2.2:8000
```

## نکات
- هیچ کلید Providerی در اپ نیست؛ فقط با Backend خودمان ارتباط دارد.
- توکن‌ها در `flutter_secure_storage` ذخیره و به‌صورت خودکار refresh می‌شوند.
- برای فارسی، فونت Vazirmatn را در `assets/fonts` اضافه و در `pubspec.yaml` معرفی کنید.

## قدم بعد (Phase 3)
Navigation (go_router) + صفحات Auth (OTP) + Home + Chat با اتصال به `ApiClient`.
