package ir.atom313.app.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.data.repository.CatalogRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.domain.model.OfficeDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OfficeMessageForm(
    val body: String = "",
    val name: String = "",
    val phone: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
)

@HiltViewModel
class OfficeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L
    private val reload = MutableStateFlow(0)

    val state: StateFlow<LoadState<OfficeDetail>> = reload
        .flatMapLatest { catalog.office(officeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    val isSignedIn: Boolean get() = session.isSignedIn

    private val _form = MutableStateFlow(
        OfficeMessageForm(
            name = session.currentUser?.name.orEmpty(),
            phone = session.currentUser?.phone.orEmpty(),
        ),
    )
    val form: StateFlow<OfficeMessageForm> = _form.asStateFlow()

    fun retry() { reload.value++ }
    fun setBody(v: String) { _form.value = _form.value.copy(body = v, errors = _form.value.errors - "body") }
    fun setName(v: String) { _form.value = _form.value.copy(name = v, errors = _form.value.errors - "name") }
    fun setPhone(v: String) { _form.value = _form.value.copy(phone = v, errors = _form.value.errors - "phone") }

    fun send() {
        val f = _form.value
        val errors = buildMap {
            if (f.body.trim().length < 3) put("body", "متن پیام را بنویسید.")
            if (!session.isSignedIn && !Validators.isMobile(f.phone)) put("phone", "شمارهٔ موبایل معتبر وارد کنید.")
        }
        if (errors.isNotEmpty()) { _form.value = f.copy(errors = errors); return }
        viewModelScope.launch {
            _form.value = f.copy(sending = true, generalError = null)
            when (val r = catalog.messageOffice(officeId, f.body, f.name, f.phone)) {
                is Outcome.Success -> _form.value = OfficeMessageForm(name = f.name, phone = f.phone, sent = true)
                is Outcome.Failure -> _form.value = f.copy(
                    sending = false,
                    errors = (r.error as? AppError.Validation)?.fields.orEmpty(),
                    generalError = r.error.message,
                )
            }
        }
    }
}
