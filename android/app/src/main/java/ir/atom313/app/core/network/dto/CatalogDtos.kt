package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Long,
    val title: String,
    val category: String = "",
    val seller: String = "",
    val sellerId: Long? = null,
    val price: Long = 0,
    val stock: Int = 0,
    val inStock: Boolean = false,
    val image: String? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val createdAt: String? = null,
    val description: String? = null,
    val available: Boolean = true,
)

@Serializable
data class ReviewDto(val id: Long, val author: String = "", val body: String = "", val rating: Int = 5, val createdAt: String? = null)

@Serializable
data class ProductDetailDto(
    val product: ProductDto,
    val reviews: List<ReviewDto> = emptyList(),
    val ratingDistribution: List<Int> = emptyList(),
    val related: List<ProductDto> = emptyList(),
)

@Serializable
data class ReviewRequest(val body: String, val rating: Int)

@Serializable
data class CategoryDto(val name: String, val count: Int = 0)

@Serializable
data class SlideDto(
    val id: Long,
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    val image: String? = null,
    val ctaLabel: String = "",
    val ctaLink: String = "",
)

@Serializable
data class AnnouncementDto(val id: Long, val title: String = "", val text: String = "", val link: String = "", val ctaLabel: String = "")

@Serializable
data class SellerDto(
    val id: Long,
    val name: String,
    val category: String = "",
    val city: String = "",
    val rating: Double = 5.0,
    val sales: Int = 0,
    val avatar: String? = null,
    val bio: String? = null,
    val createdAt: String? = null,
    val productCount: Int = 0,
)

@Serializable
data class SellerEnvelope(val seller: SellerDto)

@Serializable
data class OfficeDto(
    val id: Long,
    val name: String,
    val manager: String = "",
    val area: String = "",
    val city: String = "",
    val avatar: String? = null,
    val rating: Double = 5.0,
    val address: String = "",
    val phone: String = "",
    val bio: String = "",
    val createdAt: String? = null,
)

@Serializable
data class OfficeServiceDto(val id: Long, val title: String, val category: String = "", val description: String = "", val price: Long = 0)

@Serializable
data class OfficeDetailDto(val office: OfficeDto, val services: List<OfficeServiceDto> = emptyList())

@Serializable
data class OfficeMessageRequest(val body: String, val name: String? = null, val phone: String? = null)

@Serializable
data class ArticleDto(
    val id: Long,
    val title: String,
    val category: String = "",
    val author: String = "",
    val excerpt: String = "",
    val cover: String? = null,
    val views: Int = 0,
    val createdAt: String? = null,
    val body: String = "",
)

@Serializable
data class ArticleEnvelope(val article: ArticleDto)

@Serializable
data class FaqDto(val id: Long, val question: String, val answer: String = "")

@Serializable
data class HomeDto(
    val slides: List<SlideDto> = emptyList(),
    val announcement: AnnouncementDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val newest: List<ProductDto> = emptyList(),
    val popular: List<ProductDto> = emptyList(),
    val sellers: List<SellerDto> = emptyList(),
    val offices: List<OfficeDto> = emptyList(),
    val articles: List<ArticleDto> = emptyList(),
)
