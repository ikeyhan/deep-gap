package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
data class ErrorBody(
    val code: String = "error",
    val message: String = "",
    val fields: Map<String, String> = emptyMap(),
)

@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ItemsDto<T>(val items: List<T> = emptyList())

@Serializable
data class OkDto(val ok: Boolean = true)
