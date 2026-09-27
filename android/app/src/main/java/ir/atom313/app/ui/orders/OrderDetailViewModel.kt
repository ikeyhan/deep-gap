package ir.atom313.app.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.data.repository.OrderRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.domain.model.Order
import ir.atom313.app.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrderRepository,
) : ViewModel() {
    val code: String = Routes.dec(savedStateHandle.get<String>("code"))
    private val reload = MutableStateFlow(0)

    val state: StateFlow<LoadState<Order>> = reload
        .flatMapLatest { orders.order(code) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    fun retry() { reload.value++ }
}

data class TrackUiState(
    val code: String = "",
    val phone: String = "",
    val loading: Boolean = false,
    val order: Order? = null,
    val error: String? = null,
    val claimed: Boolean = false,
)

/** پیگیری سفارش با کد + موبایل (مانند track.html سایت) و اتصال آن به حساب. */
@HiltViewModel
class TrackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrderRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrackUiState(
            code = Routes.dec(savedStateHandle.get<String>("code")),
            phone = session.currentUser?.phone.orEmpty(),
        ),
    )
    val state: StateFlow<TrackUiState> = _state.asStateFlow()
    val isSignedIn: Boolean get() = session.isSignedIn

    fun setCode(v: String) { _state.value = _state.value.copy(code = v, error = null) }
    fun setPhone(v: String) { _state.value = _state.value.copy(phone = v, error = null) }

    fun track() {
        val s = _state.value
        if (s.code.isBlank() || !Validators.isMobile(s.phone)) {
            _state.value = s.copy(error = "کد سفارش و شمارهٔ موبایل معتبر را وارد کنید.")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(loading = true, error = null)
            when (val r = orders.track(s.code, s.phone)) {
                is Outcome.Success -> _state.value = _state.value.copy(loading = false, order = r.value)
                is Outcome.Failure -> _state.value = _state.value.copy(loading = false, error = r.error.message, order = null)
            }
        }
    }

    /** اتصال سفارش مهمان به حساب کاربر (همان مدرک پیگیری). */
    fun claim(onMessage: (String) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            when (val r = orders.claim(s.code, s.phone)) {
                is Outcome.Success -> { _state.value = _state.value.copy(order = r.value, claimed = true); onMessage("سفارش به حساب شما اضافه شد.") }
                is Outcome.Failure -> onMessage(r.error.message)
            }
        }
    }
}
