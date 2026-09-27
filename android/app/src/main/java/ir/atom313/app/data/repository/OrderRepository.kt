package ir.atom313.app.data.repository

import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.common.map
import ir.atom313.app.core.database.CacheStore
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.CartItemRequest
import ir.atom313.app.core.network.dto.ClaimOrderRequest
import ir.atom313.app.core.network.dto.OrderDto
import ir.atom313.app.core.network.dto.OrderItemDto
import ir.atom313.app.core.network.dto.PageDto
import ir.atom313.app.core.network.dto.PlaceOrderRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.data.mapper.toMyProduct
import ir.atom313.app.data.paging.Page
import ir.atom313.app.domain.model.CartLine
import ir.atom313.app.domain.model.CheckoutForm
import ir.atom313.app.domain.model.MyProduct
import ir.atom313.app.domain.model.MyProductsFilter
import ir.atom313.app.domain.model.Order
import ir.atom313.app.domain.model.PlacedOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** سفارش‌ها و «محصولات من». صفحهٔ اول هر فهرست برای نمایش آفلاین کش می‌شود. */
@Singleton
class OrderRepository @Inject constructor(
    private val api: AtomApi,
    private val cache: CacheStore,
) {
    suspend fun place(form: CheckoutForm, lines: List<CartLine>, coupon: String?, idempotencyKey: String): Outcome<PlacedOrder> =
        apiCall {
            api.placeOrder(
                idempotencyKey,
                PlaceOrderRequest(
                    items = lines.map { CartItemRequest(it.productId, it.qty) },
                    coupon = coupon?.trim()?.ifBlank { null },
                    name = form.name.trim(), phone = Validators.normalizeMobile(form.phone),
                    city = form.city.trim(), address = form.address.trim(), note = form.note.trim(),
                ),
            )
        }.map { it.toDomain() }

    suspend fun orders(page: Int, limit: Int = 20): Outcome<Page<Order>> {
        val r = apiCall { api.orders(page, limit) }
        if (page == 1 && r is Outcome.Success) cache.write("orders:1", PageDto.serializer(OrderDto.serializer()), r.value, personal = true)
        return r.map { Page(it.items.map { o -> o.toDomain() }, it.hasMore) }
    }

    suspend fun cachedOrders(): List<Order>? =
        cache.read("orders:1", PageDto.serializer(OrderDto.serializer()))?.items?.map { it.toDomain() }

    fun order(code: String): Flow<LoadState<Order>> =
        cache.cached("order:$code", OrderDto.serializer(), personal = true) { apiCall { api.order(code).order } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    suspend fun myProducts(page: Int, filter: MyProductsFilter, limit: Int = 20): Outcome<Page<MyProduct>> {
        val r = apiCall { api.myProducts(page, limit, filter.apiValue) }
        val key = "myproducts:${filter.name}:1"
        if (page == 1 && r is Outcome.Success) cache.write(key, PageDto.serializer(OrderItemDto.serializer()), r.value, personal = true)
        return r.map { Page(it.items.map { i -> i.toMyProduct() }, it.hasMore) }
    }

    suspend fun cachedMyProducts(filter: MyProductsFilter): List<MyProduct>? =
        cache.read("myproducts:${filter.name}:1", PageDto.serializer(OrderItemDto.serializer()))?.items?.map { it.toMyProduct() }

    suspend fun claim(code: String, phone: String): Outcome<Order> =
        apiCall { api.claimOrder(ClaimOrderRequest(Validators.latinDigits(code).trim(), Validators.normalizeMobile(phone))) }
            .map { it.order.toDomain() }

    suspend fun track(code: String, phone: String): Outcome<Order> =
        apiCall { api.track(Validators.latinDigits(code).filter(Char::isDigit), Validators.normalizeMobile(phone)) }
            .map { it.order.toDomain() }
}
