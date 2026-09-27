package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class WishlistDto(val items: List<ProductDto> = emptyList(), val ids: List<Long> = emptyList())

@Serializable
data class WishlistMergeRequest(val productIds: List<Long>)

@Serializable
data class WishlistIdsDto(val ids: List<Long> = emptyList())

@Serializable
data class NotificationDto(
    val id: Long,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val ref: String = "",
    val read: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class NotificationPageDto(
    val items: List<NotificationDto> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val unread: Int = 0,
)

@Serializable
data class MarkReadRequest(val ids: List<Long> = emptyList(), val all: Boolean = false)

@Serializable
data class DeviceRequest(val token: String, val provider: String = "none", val appVersion: String = "")

@Serializable
data class TicketDto(
    val id: Long,
    val subject: String = "",
    val body: String = "",
    val reply: String = "",
    val status: String = "open",
    val createdAt: String? = null,
)

@Serializable
data class TicketEnvelope(val item: TicketDto)

@Serializable
data class NewTicketRequest(val subject: String, val body: String)

@Serializable
data class OfficeThreadDto(
    val id: Long,
    val office: String = "",
    val body: String = "",
    val reply: String = "",
    val status: String = "open",
    val createdAt: String? = null,
)

@Serializable
data class ChatMessageDto(val role: String, val content: String)

@Serializable
data class ChatRequest(val messages: List<ChatMessageDto>, val session: String)

@Serializable
data class ChatResponse(val reply: String = "", val session: String? = null, val error: String? = null)
