package ir.atom313.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * کش پاسخ‌های API به‌صورت JSON خام برای نمایش آفلاین و بارگذاری فوری
 * (stale-while-revalidate). دادهٔ شخصی با [personal]=true علامت می‌خورد و هنگام خروج پاک می‌شود.
 */
@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long,
    val personal: Boolean,
)

/** اقلام سبد خرید (محلی). قیمت ذخیره‌شده فقط برای نمایش سریع است؛ مبلغ نهایی را سرور محاسبه می‌کند. */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Long,
    val title: String,
    val image: String?,
    val seller: String,
    val price: Long,
    val qty: Int,
    val addedAt: Long,
)
