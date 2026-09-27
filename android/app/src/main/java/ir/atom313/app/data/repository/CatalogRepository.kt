package ir.atom313.app.data.repository

import androidx.paging.PagingData
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.common.map
import ir.atom313.app.core.database.CacheStore
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.HomeDto
import ir.atom313.app.core.network.dto.OfficeDetailDto
import ir.atom313.app.core.network.dto.OfficeMessageRequest
import ir.atom313.app.core.network.dto.ProductDetailDto
import ir.atom313.app.core.network.dto.ReviewRequest
import ir.atom313.app.core.network.dto.SellerDto
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.data.paging.Page
import ir.atom313.app.data.paging.pager
import ir.atom313.app.domain.model.Category
import ir.atom313.app.domain.model.Home
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.OfficeDetail
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductDetail
import ir.atom313.app.domain.model.ProductFilter
import ir.atom313.app.domain.model.Seller
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** دادهٔ فروشگاه (عمومی). صفحهٔ خانه و جزئیات با کش آفلاین؛ فهرست‌ها با صفحه‌بندی. */
@Singleton
class CatalogRepository @Inject constructor(
    private val api: AtomApi,
    private val cache: CacheStore,
) {
    fun home(): Flow<LoadState<Home>> =
        cache.cached("home", HomeDto.serializer()) { apiCall { api.home() } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    fun products(filter: ProductFilter): Flow<PagingData<Product>> = pager(pageSize = 20) { page, limit ->
        apiCall {
            api.products(
                page = page, limit = limit,
                query = filter.query.trim().takeIf { it.isNotEmpty() },
                category = filter.category, sellerId = filter.sellerId,
                minPrice = filter.minPrice, maxPrice = filter.maxPrice,
                inStock = if (filter.inStockOnly) "1" else null,
                sort = filter.sort.apiValue,
            )
        }.map { Page(it.items.map { p -> p.toDomain() }, it.hasMore) }
    }

    fun product(id: Long): Flow<LoadState<ProductDetail>> =
        cache.cached("product:$id", ProductDetailDto.serializer()) { apiCall { api.product(id) } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    suspend fun addReview(productId: Long, body: String, rating: Int): Outcome<Unit> =
        apiCall { api.addReview(productId, ReviewRequest(body.trim(), rating)) }.map { }

    suspend fun categories(): Outcome<List<Category>> = apiCall { api.categories() }.map { r -> r.items.map { it.toDomain() } }

    fun sellers(query: String): Flow<PagingData<Seller>> = pager { page, limit ->
        apiCall { api.sellers(page, limit, query.trim().takeIf { it.isNotEmpty() }) }
            .map { Page(it.items.map { s -> s.toDomain() }, it.hasMore) }
    }

    fun seller(id: Long): Flow<LoadState<Seller>> =
        cache.cached("seller:$id", SellerDto.serializer()) { apiCall { api.seller(id).seller } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    fun offices(query: String): Flow<PagingData<Office>> = pager { page, limit ->
        apiCall { api.offices(page, limit, query.trim().takeIf { it.isNotEmpty() }) }
            .map { Page(it.items.map { o -> o.toDomain() }, it.hasMore) }
    }

    fun office(id: Long): Flow<LoadState<OfficeDetail>> =
        cache.cached("office:$id", OfficeDetailDto.serializer()) { apiCall { api.office(id) } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    suspend fun messageOffice(officeId: Long, body: String, name: String?, phone: String?): Outcome<Unit> =
        apiCall {
            api.messageOffice(officeId, OfficeMessageRequest(body.trim(), name?.trim()?.ifBlank { null }, phone?.let(Validators::normalizeMobile)?.ifBlank { null }))
        }.map { }
}
