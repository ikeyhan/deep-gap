package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CartItemRequest(val productId: Long, val qty: Int)

@Serializable
data class QuoteRequest(val items: List<CartItemRequest>, val coupon: String? = null)

@Serializable
data class QuoteLineDto(
    val productId: Long? = null,
    val title: String = "",
    val seller: String = "",
    val image: String? = null,
    val price: Long = 0,
    val qty: Int = 1,
    val amount: Long = 0,
    val stock: Int = 0,
    val available: Boolean = true,
    val inStock: Boolean = true,
)

@Serializable
data class CouponDto(val code: String, val title: String = "", val kind: String = "percent", val amount: Long = 0)

@Serializable
data class QuoteDto(
    val lines: List<QuoteLineDto> = emptyList(),
    val subtotal: Long = 0,
    val discount: Long = 0,
    val shipping: Long = 0,
    val total: Long = 0,
    val minOrder: Long = 0,
    val freeShippingMin: Long = 0,
    val coupon: CouponDto? = null,
    val couponError: String? = null,
)

@Serializable
data class PlaceOrderRequest(
    val items: List<CartItemRequest>,
    val coupon: String? = null,
    val name: String,
    val phone: String,
    val city: String,
    val address: String,
    val note: String = "",
)

@Serializable
data class OrderItemDto(
    val id: Long,
    val productId: Long? = null,
    val title: String = "",
    val qty: Int = 1,
    val seller: String = "",
    val amount: Long = 0,
    val status: String = "pending",
    val image: String? = null,
    // فقط در «محصولات من»
    val orderCode: String? = null,
    val purchasedAt: String? = null,
    val canReorder: Boolean = false,
)

@Serializable
data class OrderDto(
    val code: String,
    val status: String = "pending",
    val createdAt: String? = null,
    val total: Long = 0,
    val itemCount: Int = 0,
    val items: List<OrderItemDto> = emptyList(),
    val note: String = "",
    val recipient: String? = null,
    val phone: String? = null,
    val address: String? = null,
)

@Serializable
data class OrderEnvelope(val order: OrderDto)

@Serializable
data class PlaceOrderResponse(
    val order: OrderDto,
    val subtotal: Long = 0,
    val discount: Long = 0,
    val shipping: Long = 0,
    val total: Long = 0,
)

@Serializable
data class ClaimOrderRequest(val code: String, val phone: String)
