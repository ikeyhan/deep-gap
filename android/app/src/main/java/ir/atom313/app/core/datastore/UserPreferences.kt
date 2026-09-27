package ir.atom313.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** آخرین سفارش مهمان — برای پیگیری سریع و اتصال به حساب پس از ورود. */
data class GuestOrder(val code: String, val phone: String)

private val Context.prefs: DataStore<Preferences> by preferencesDataStore(name = "prefs")

/** تنظیمات غیرحساس اپ (بدون توکن؛ توکن‌ها در TokenStore رمز می‌شوند). */
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val guestWishKey = stringSetPreferencesKey("guest_wishlist")
    private val guestOrderKey = stringPreferencesKey("guest_order")
    private val lastNotifKey = longPreferencesKey("last_notification_id")
    private val installIdKey = stringPreferencesKey("install_id")
    private val recentSearchKey = stringPreferencesKey("recent_searches")

    val theme: Flow<ThemeMode> = context.prefs.data.map { p ->
        p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    suspend fun setTheme(mode: ThemeMode) = context.prefs.edit { it[themeKey] = mode.name }

    val guestWishlist: Flow<Set<Long>> = context.prefs.data.map { p -> p[guestWishKey].orEmpty().mapNotNull(String::toLongOrNull).toSet() }
    suspend fun setGuestWishlist(ids: Set<Long>) = context.prefs.edit { it[guestWishKey] = ids.map(Long::toString).toSet() }

    val guestOrder: Flow<GuestOrder?> = context.prefs.data.map { p ->
        p[guestOrderKey]?.split('|')?.takeIf { it.size == 2 }?.let { GuestOrder(it[0], it[1]) }
    }
    suspend fun setGuestOrder(order: GuestOrder?) = context.prefs.edit {
        if (order == null) it.remove(guestOrderKey) else it[guestOrderKey] = "${order.code}|${order.phone}"
    }

    suspend fun lastNotificationId(): Long = context.prefs.data.first()[lastNotifKey] ?: 0L
    suspend fun setLastNotificationId(id: Long) = context.prefs.edit { it[lastNotifKey] = id }

    /** شناسهٔ تصادفی نصب (بدون شناسهٔ سخت‌افزاری) برای ثبت دستگاه اعلان‌ها. */
    suspend fun installId(): String {
        context.prefs.data.first()[installIdKey]?.let { return it }
        val id = "and-" + UUID.randomUUID().toString()
        context.prefs.edit { it[installIdKey] = id }
        return id
    }

    val recentSearches: Flow<List<String>> = context.prefs.data.map { p ->
        p[recentSearchKey]?.split('\n')?.filter(String::isNotBlank).orEmpty()
    }
    suspend fun addRecentSearch(q: String) = context.prefs.edit { p ->
        val list = (listOf(q.trim()) + p[recentSearchKey]?.split('\n').orEmpty()).filter(String::isNotBlank).distinct().take(8)
        p[recentSearchKey] = list.joinToString("\n")
    }
    suspend fun clearRecentSearches() = context.prefs.edit { it.remove(recentSearchKey) }

    /** هنگام خروج: داده‌های مرتبط با حساب پاک می‌شود (تم و شناسهٔ نصب می‌ماند). */
    suspend fun clearAccountData() = context.prefs.edit {
        it.remove(lastNotifKey)
    }
}
