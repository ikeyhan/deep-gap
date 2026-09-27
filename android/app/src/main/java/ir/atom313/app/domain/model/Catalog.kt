package ir.atom313.app.domain.model

data class Product(
    val id: Long,
    val title: String,
    val category: String,
    val seller: String,
    val sellerId: Long?,
    val price: Long,
    val stock: Int,
    val inStock: Boolean,
    val image: String?,
    val rating: Double?,
    val reviewCount: Int,
    val description: String = "",
    val available: Boolean = true,
)

data class Review(val id: Long, val author: String, val body: String, val rating: Int, val createdAt: String?)

data class ProductDetail(
    val product: Product,
    val reviews: List<Review>,
    /** تعداد نظرات ۱ تا ۵ ستاره */
    val ratingDistribution: List<Int>,
    val related: List<Product>,
)

data class Category(val name: String, val count: Int)

data class Slide(val id: Long, val eyebrow: String, val title: String, val subtitle: String, val image: String?, val ctaLabel: String, val ctaLink: String)

data class Announcement(val title: String, val text: String, val link: String, val ctaLabel: String)

data class Seller(
    val id: Long,
    val name: String,
    val category: String,
    val city: String,
    val rating: Double,
    val sales: Int,
    val avatar: String?,
    val bio: String = "",
    val createdAt: String? = null,
    val productCount: Int = 0,
)

data class Office(
    val id: Long,
    val name: String,
    val manager: String,
    val area: String,
    val city: String,
    val avatar: String?,
    val rating: Double,
    val address: String = "",
    val phone: String = "",
    val bio: String = "",
)

data class OfficeService(val id: Long, val title: String, val category: String, val description: String, val price: Long)

data class OfficeDetail(val office: Office, val services: List<OfficeService>)

data class Article(
    val id: Long,
    val title: String,
    val category: String,
    val author: String,
    val excerpt: String,
    val cover: String?,
    val views: Int,
    val createdAt: String?,
    val bodyHtml: String = "",
)

data class Faq(val id: Long, val question: String, val answer: String)

data class Home(
    val slides: List<Slide>,
    val announcement: Announcement?,
    val categories: List<Category>,
    val newest: List<Product>,
    val popular: List<Product>,
    val sellers: List<Seller>,
    val offices: List<Office>,
    val articles: List<Article>,
)

enum class ProductSort(val apiValue: String) { NEWEST("newest"), POPULAR("popular"), PRICE_ASC("price_asc"), PRICE_DESC("price_desc"), RATING("rating") }

data class ProductFilter(
    val query: String = "",
    val category: String? = null,
    val sellerId: Long? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val inStockOnly: Boolean = false,
    val sort: ProductSort = ProductSort.NEWEST,
) {
    /** تعداد فیلترهای فعال (برای نشان روی دکمهٔ فیلتر) */
    val activeCount: Int get() = listOf(category != null, minPrice != null || maxPrice != null, inStockOnly, sort != ProductSort.NEWEST).count { it }
}
