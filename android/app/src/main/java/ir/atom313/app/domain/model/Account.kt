package ir.atom313.app.domain.model

enum class NotificationType { ORDER_PLACED, ORDER_STATUS, SUPPORT_REPLY, OFFICE_REPLY, OTHER;
    companion object {
        fun from(raw: String) = when (raw) {
            "order_placed" -> ORDER_PLACED
            "order_status" -> ORDER_STATUS
            "support_reply" -> SUPPORT_REPLY
            "office_reply" -> OFFICE_REPLY
            else -> OTHER
        }
    }
}

data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val ref: String,
    val read: Boolean,
    val createdAt: String?,
)

enum class TicketStatus(val label: String) { OPEN("باز"), PENDING("در حال بررسی"), CLOSED("بسته");
    companion object {
        fun from(raw: String) = when (raw) { "pending" -> PENDING; "closed" -> CLOSED; else -> OPEN }
    }
}

data class Ticket(val id: Long, val subject: String, val body: String, val reply: String, val status: TicketStatus, val createdAt: String?)

data class OfficeThread(val id: Long, val office: String, val body: String, val reply: String, val replied: Boolean, val createdAt: String?)

data class ChatMessage(val fromUser: Boolean, val text: String)

data class AppConfig(
    val siteName: String = "اتم ۳۱۳",
    val siteDescription: String = "",
    val domain: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val instagram: String = "",
    val telegram: String = "",
    val whatsapp: String = "",
    val shippingCost: Long = 0,
    val freeShippingMin: Long = 0,
    val minOrder: Long = 0,
    val onlinePayment: Boolean = false,
    val paymentNote: String = "پس از ثبت سفارش، هماهنگی پرداخت و ارسال با شما انجام می‌شود.",
    val chatEnabled: Boolean = true,
    val aiChatEnabled: Boolean = false,
    val chatWelcome: String = "",
    val registrationOpen: Boolean = true,
    val maintenance: Boolean = false,
    val minVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
)
