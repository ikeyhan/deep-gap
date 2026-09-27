package ir.atom313.app.domain.model

/** قلم سبد محلی (قیمت ذخیره‌شده فقط برای نمایش سریع پیش از پاسخ سرور است). */
data class CartLine(
    val productId: Long,
    val title: String,
    val image: String?,
    val seller: String,
    val localPrice: Long,
    val qty: Int,
)

data class QuoteLine(
    val productId: Long?,
    val title: String,
    val price: Long,
    val qty: Int,
    val amount: Long,
    val stock: Int,
    val available: Boolean,
    val inStock: Boolean,
)

data class Coupon(val code: String, val title: String, val kind: String, val amount: Long)

/** پیش‌فاکتور سرور — تنها مرجع مبالغ. */
data class CartQuote(
    val lines: List<QuoteLine>,
    val subtotal: Long,
    val discount: Long,
    val shipping: Long,
    val total: Long,
    val minOrder: Long,
    val freeShippingMin: Long,
    val coupon: Coupon?,
    val couponError: String?,
) {
    val hasProblems: Boolean get() = lines.any { !it.available || !it.inStock }
    val belowMinimum: Boolean get() = subtotal in 1 until minOrder
    /** مانده تا ارسال رایگان */
    val toFreeShipping: Long get() = if (shipping > 0 && freeShippingMin > 0) (freeShippingMin - (subtotal - discount)).coerceAtLeast(0) else 0
}
