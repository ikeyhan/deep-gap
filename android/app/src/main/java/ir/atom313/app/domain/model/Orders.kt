package ir.atom313.app.domain.model

/** وضعیت سفارش — همان مقادیر سایت و پنل مدیریت. */
enum class OrderStatus(val apiValue: String, val label: String, val step: Int) {
    PENDING("pending", "در انتظار تأیید", 0),
    REVIEW("review", "در حال بررسی", 1),
    SHIPPING("shipping", "در حال ارسال", 2),
    DELIVERED("delivered", "تحویل شد", 3),
    RETURNED("returned", "مرجوع شد", -1);

    val isActive: Boolean get() = this == PENDING || this == REVIEW || this == SHIPPING

    companion object {
        fun from(raw: String): OrderStatus = entries.firstOrNull { it.apiValue == raw } ?: PENDING
        /** مراحل خط زمانی پیگیری */
        val timeline = listOf("ثبت سفارش", "تأیید و آماده‌سازی", "تحویل به پست", "تحویل شد")
    }
}

data class OrderItem(
    val id: Long,
    val productId: Long?,
    val title: String,
    val qty: Int,
    val seller: String,
    val amount: Long,
    val status: OrderStatus,
    val image: String?,
)

data class Order(
    val code: String,
    val status: OrderStatus,
    val createdAt: String?,
    val total: Long,
    val itemCount: Int,
    val items: List<OrderItem>,
    val note: String,
    val recipient: String?,
    val phone: String?,
    val address: String?,
)

/** قلم خریداری‌شده در «محصولات من». */
data class MyProduct(
    val item: OrderItem,
    val orderCode: String,
    val purchasedAt: String?,
    val canReorder: Boolean,
)

enum class MyProductsFilter(val apiValue: String?, val label: String) {
    ALL(null, "همه"), ACTIVE("active", "در جریان"), DELIVERED("delivered", "تحویل‌شده"), RETURNED("returned", "مرجوعی")
}

data class PlacedOrder(val order: Order, val subtotal: Long, val discount: Long, val shipping: Long, val total: Long)

data class CheckoutForm(
    val name: String = "",
    val phone: String = "",
    val city: String = "",
    val address: String = "",
    val note: String = "",
)
