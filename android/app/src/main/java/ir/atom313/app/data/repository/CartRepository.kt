package ir.atom313.app.data.repository

import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.map
import ir.atom313.app.core.database.CartDao
import ir.atom313.app.core.database.CartItemEntity
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.CartItemRequest
import ir.atom313.app.core.network.dto.QuoteRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.domain.model.CartLine
import ir.atom313.app.domain.model.CartQuote
import ir.atom313.app.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** سبد خرید محلی (پایدار، آفلاین) + پیش‌فاکتور سمت سرور. */
@Singleton
class CartRepository @Inject constructor(
    private val dao: CartDao,
    private val api: AtomApi,
) {
    val lines: Flow<List<CartLine>> = dao.observe().map { list ->
        list.map { CartLine(it.productId, it.title, it.image, it.seller, it.price, it.qty) }
    }
    val count: Flow<Int> = lines.map { l -> l.sumOf { it.qty } }

    /** افزودن یا افزایش تعداد (سقف ۹۹ مانند سرور). */
    suspend fun add(product: Product, qty: Int = 1) {
        val cur = dao.get(product.id)
        val next = ((cur?.qty ?: 0) + qty).coerceIn(1, MAX_QTY)
        dao.upsert(
            CartItemEntity(
                productId = product.id, title = product.title, image = product.image, seller = product.seller,
                price = product.price, qty = next, addedAt = cur?.addedAt ?: System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setQty(productId: Long, qty: Int) {
        if (qty <= 0) dao.remove(productId) else dao.setQty(productId, qty.coerceAtMost(MAX_QTY))
    }

    suspend fun remove(productId: Long) = dao.remove(productId)
    suspend fun clear() = dao.clear()
    suspend fun snapshot(): List<CartLine> = dao.all().map { CartLine(it.productId, it.title, it.image, it.seller, it.price, it.qty) }

    suspend fun quote(lines: List<CartLine>, coupon: String?): Outcome<CartQuote> =
        apiCall { api.quote(QuoteRequest(lines.map { CartItemRequest(it.productId, it.qty) }, coupon?.trim()?.ifBlank { null })) }
            .map { it.toDomain() }

    companion object { const val MAX_QTY = 99 }
}
