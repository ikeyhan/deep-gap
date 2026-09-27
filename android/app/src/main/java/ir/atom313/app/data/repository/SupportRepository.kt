package ir.atom313.app.data.repository

import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.map
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.ChatMessageDto
import ir.atom313.app.core.network.dto.ChatRequest
import ir.atom313.app.core.network.dto.NewTicketRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.domain.model.ChatMessage
import ir.atom313.app.domain.model.OfficeThread
import ir.atom313.app.domain.model.Ticket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** پشتیبانی: تیکت‌ها (پاسخ از پنل مدیریت)، گفتگو با دفاتر و دستیار هوشمند. */
@Singleton
class SupportRepository @Inject constructor(private val api: AtomApi) {
    private val chatSession = "app-" + UUID.randomUUID().toString().take(12)

    suspend fun tickets(): Outcome<List<Ticket>> = apiCall { api.tickets() }.map { r -> r.items.map { it.toDomain() } }

    suspend fun newTicket(subject: String, body: String): Outcome<Ticket> =
        apiCall { api.newTicket(NewTicketRequest(subject.trim(), body.trim())) }.map { it.item.toDomain() }

    suspend fun officeThreads(): Outcome<List<OfficeThread>> = apiCall { api.officeThreads() }.map { r -> r.items.map { it.toDomain() } }

    /** گفتگو با دستیار هوشمند (کلید فقط روی سرور است). ۱۲ پیام آخر ارسال می‌شود. */
    suspend fun chat(history: List<ChatMessage>): Outcome<String> =
        apiCall {
            api.chat(ChatRequest(history.takeLast(12).map { ChatMessageDto(if (it.fromUser) "user" else "assistant", it.text) }, chatSession))
        }.map { it.reply }
}
