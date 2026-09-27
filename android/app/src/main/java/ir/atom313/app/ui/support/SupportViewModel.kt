package ir.atom313.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.data.repository.SupportRepository
import ir.atom313.app.domain.model.ChatMessage
import ir.atom313.app.domain.model.OfficeThread
import ir.atom313.app.domain.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(
    val tickets: LoadState<List<Ticket>> = LoadState.loading(),
    val officeThreads: List<OfficeThread> = emptyList(),
    val subject: String = "",
    val body: String = "",
    val sending: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val support: SupportRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportUiState())
    val state: StateFlow<SupportUiState> = _state.asStateFlow()
    val isSignedIn: Boolean get() = session.isSignedIn

    init { load() }

    fun load() {
        if (!session.isSignedIn) { _state.value = _state.value.copy(tickets = LoadState(emptyList())); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(tickets = LoadState.loading(_state.value.tickets.data))
            when (val r = support.tickets()) {
                is Outcome.Success -> _state.value = _state.value.copy(tickets = LoadState(r.value))
                is Outcome.Failure -> _state.value = _state.value.copy(tickets = LoadState(_state.value.tickets.data, error = r.error))
            }
            (support.officeThreads() as? Outcome.Success)?.let { _state.value = _state.value.copy(officeThreads = it.value) }
        }
    }

    fun setSubject(v: String) { _state.value = _state.value.copy(subject = v, errors = emptyMap(), generalError = null) }
    fun setBody(v: String) { _state.value = _state.value.copy(body = v, errors = emptyMap(), generalError = null) }

    fun send(onSent: () -> Unit) {
        val s = _state.value
        if (s.body.trim().length < 5) {
            _state.value = s.copy(errors = mapOf("body" to "متن پیام را کامل‌تر بنویسید."))
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(sending = true)
            when (val r = support.newTicket(s.subject, s.body)) {
                is Outcome.Success -> {
                    _state.value = _state.value.copy(
                        sending = false, subject = "", body = "",
                        tickets = LoadState(listOf(r.value) + _state.value.tickets.data.orEmpty()),
                    )
                    onSent()
                }
                is Outcome.Failure -> _state.value = _state.value.copy(
                    sending = false,
                    errors = (r.error as? AppError.Validation)?.fields.orEmpty(),
                    generalError = r.error.message,
                )
            }
        }
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

/** دستیار هوشمند پشتیبانی — همان سرویس ویجت سایت (کلید فقط روی سرور). */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val support: SupportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun start(welcome: String) {
        if (_state.value.messages.isEmpty() && welcome.isNotBlank()) {
            _state.value = _state.value.copy(messages = listOf(ChatMessage(fromUser = false, text = welcome)))
        }
    }

    fun setInput(v: String) { _state.value = _state.value.copy(input = v, error = null) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return
        val history = _state.value.messages + ChatMessage(fromUser = true, text = text)
        _state.value = _state.value.copy(messages = history, input = "", sending = true, error = null)
        viewModelScope.launch {
            when (val r = support.chat(history)) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(fromUser = false, text = r.value),
                    sending = false,
                )
                is Outcome.Failure -> _state.value = _state.value.copy(sending = false, error = r.error.message)
            }
        }
    }
}
