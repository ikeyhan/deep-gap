package ir.atom313.app.notifications

import ir.atom313.app.data.repository.NotificationRepository
import ir.atom313.app.domain.model.AppNotification
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نقطهٔ اتصال پوش. وقتی FCM (یا سرویس پوش داخلی) اضافه شود، سرویس آن فقط این دو متد را صدا می‌زند:
 *  - [onNewToken]: ثبت توکن روی سرور (جدول devices با provider=fcm)
 *  - [onMessage]: نمایش اعلان با همان Publisher و همان پیوندهای داخلی
 * بقیهٔ اپ (صفحهٔ اعلان‌ها، شمارندهٔ نخوانده، پیوندها) نیازی به تغییر ندارد.
 */
@Singleton
class PushMessageHandler @Inject constructor(
    private val repository: NotificationRepository,
    private val publisher: NotificationPublisher,
) {
    suspend fun onNewToken(token: String) { repository.registerDevice(token) }
    fun onMessage(notification: AppNotification) = publisher.show(notification)
}
