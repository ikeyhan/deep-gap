package ir.atom313.app.navigation

import android.net.Uri
import ir.atom313.app.domain.model.AppNotification
import ir.atom313.app.domain.model.NotificationType

/**
 * پیوندهای ورودی: اعلان‌های اپ (atom313://) و پیوندهای سایت (https://atomy313.ir/...).
 * خروجی یک مسیر ناوبری داخلی است تا کاربر مستقیم به همان صفحه برسد.
 */
object DeepLinks {
    const val SCHEME = "atom313"

    fun forNotification(n: AppNotification): String = when (n.type) {
        NotificationType.ORDER_PLACED, NotificationType.ORDER_STATUS -> "$SCHEME://order/${n.ref}"
        NotificationType.SUPPORT_REPLY, NotificationType.OFFICE_REPLY -> "$SCHEME://support"
        NotificationType.OTHER -> "$SCHEME://notifications"
    }

    /** تبدیل هر URI ورودی به مسیر داخلی؛ null یعنی مسیر متناظری وجود ندارد. */
    fun toRoute(uri: Uri?): String? {
        if (uri == null) return null
        if (uri.scheme == SCHEME) {
            val host = uri.host.orEmpty()
            val id = uri.pathSegments.firstOrNull().orEmpty()
            return when (host) {
                "order" -> if (id.isNotBlank()) Routes.order(id) else Routes.ORDERS
                "product" -> id.toLongOrNull()?.let(Routes::product)
                "support" -> Routes.SUPPORT
                "notifications" -> Routes.NOTIFICATIONS
                "orders" -> Routes.ORDERS
                else -> null
            }
        }
        if (uri.scheme == "https" || uri.scheme == "http") {
            val path = uri.path.orEmpty()
            return when {
                // product.html?id=12 — همان آدرس سایت
                path.startsWith("/product") -> uri.getQueryParameter("id")?.toLongOrNull()?.let(Routes::product)
                path.startsWith("/track") -> Routes.track(uri.getQueryParameter("code").orEmpty())
                else -> null
            }
        }
        return null
    }
}
