package ir.atom313.app.data.repository

import ir.atom313.app.BuildConfig
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.map
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.DeviceRequest
import ir.atom313.app.core.network.dto.MarkReadRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.data.paging.Page
import ir.atom313.app.domain.model.AppNotification
import javax.inject.Inject
import javax.inject.Singleton

/** اعلان‌های حساب (مشترک با سایت). */
@Singleton
class NotificationRepository @Inject constructor(
    private val api: AtomApi,
    private val prefs: UserPreferences,
    private val session: SessionRepository,
) {
    suspend fun page(page: Int, limit: Int = 30): Outcome<Page<AppNotification>> {
        val r = apiCall { api.notifications(page, limit) }
        if (r is Outcome.Success) session.setUnreadCount(r.value.unread)
        return r.map { Page(it.items.map { n -> n.toDomain() }, it.hasMore) }
    }

    suspend fun markRead(ids: List<Long>): Outcome<Unit> = apiCall { api.markRead(MarkReadRequest(ids = ids)) }.map { }
    suspend fun markAllRead(): Outcome<Unit> = apiCall { api.markRead(MarkReadRequest(all = true)) }.map { session.setUnreadCount(0) }

    /**
     * اعلان‌های جدیدتر از آخرین موردِ دیده‌شده (برای همگام‌سازی پس‌زمینه).
     * اولین اجرا فقط نشانگر را تنظیم می‌کند تا اعلان‌های قدیمی دوباره نمایش داده نشوند.
     */
    suspend fun fetchNew(): Outcome<List<AppNotification>> {
        val last = prefs.lastNotificationId()
        val r = apiCall { api.notifications(1, 20, since = last.takeIf { it > 0 }) }
        return when (r) {
            is Outcome.Success -> {
                session.setUnreadCount(r.value.unread)
                val items = r.value.items.map { it.toDomain() }
                items.maxOfOrNull { it.id }?.let { prefs.setLastNotificationId(it) }
                Outcome.Success(if (last == 0L) emptyList() else items.filter { !it.read })
            }
            is Outcome.Failure -> r
        }
    }

    /**
     * ثبت دستگاه. فعلاً provider=none (دریافت با همگام‌سازی دوره‌ای)؛ با افزودن FCM،
     * توکن FCM با provider=fcm از همین مسیر ثبت می‌شود.
     */
    suspend fun registerDevice(pushToken: String? = null): Outcome<Unit> {
        val token = pushToken ?: prefs.installId()
        return apiCall { api.registerDevice(DeviceRequest(token, if (pushToken != null) "fcm" else "none", BuildConfig.VERSION_NAME)) }.map { }
    }

    suspend fun deviceToken(): String = prefs.installId()
}
