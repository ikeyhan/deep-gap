package ir.atom313.app.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.CartRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.domain.model.CartQuote
import ir.atom313.app.domain.model.CheckoutForm
import ir.atom313.app.domain.usecase.PlaceOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CheckoutUiState(
    val form: CheckoutForm = CheckoutForm(),
    val quote: CartQuote? = null,
    val loadingQuote: Boolean = true,
    val submitting: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: AppError? = null,
    val placedCode: String? = null,
    val isGuest: Boolean = true,
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cart: CartRepository,
    private val placeOrder: PlaceOrderUseCase,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    /**
     * کلید یکتای ثبت سفارش. یک‌بار برای این صفحه ساخته می‌شود تا تلاش دوباره پس از
     * قطع شبکه، سفارش تکراری نسازد؛ سرور همان پاسخ قبلی را برمی‌گرداند.
     */
    private val idempotencyKey = "and-" + UUID.randomUUID().toString()
    private var coupon: String? = null

    fun start(couponCode: String?) {
        coupon = couponCode
        val user = session.currentUser
        _state.value = _state.value.copy(
            isGuest = user == null,
            form = _state.value.form.copy(
                name = _state.value.form.name.ifBlank { user?.name.orEmpty() },
                phone = _state.value.form.phone.ifBlank { user?.phone.orEmpty() },
                city = _state.value.form.city.ifBlank { user?.city.orEmpty() },
                address = _state.value.form.address.ifBlank { user?.address.orEmpty() },
            ),
        )
        refreshQuote()
    }

    fun refreshQuote() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingQuote = true, generalError = null)
            val lines = cart.snapshot()
            if (lines.isEmpty()) { _state.value = _state.value.copy(loadingQuote = false, quote = null); return@launch }
            when (val r = cart.quote(lines, coupon)) {
                is Outcome.Success -> _state.value = _state.value.copy(quote = r.value, loadingQuote = false)
                is Outcome.Failure -> _state.value = _state.value.copy(loadingQuote = false, generalError = r.error)
            }
        }
    }

    fun update(transform: (CheckoutForm) -> CheckoutForm) {
        val form = transform(_state.value.form)
        _state.value = _state.value.copy(form = form, errors = emptyMap(), generalError = null)
    }

    fun submit() {
        val s = _state.value
        val localErrors = placeOrder.validate(s.form)
        if (localErrors.isNotEmpty()) { _state.value = s.copy(errors = localErrors); return }
        viewModelScope.launch {
            _state.value = s.copy(submitting = true, generalError = null, errors = emptyMap())
            when (val r = placeOrder(s.form, coupon, idempotencyKey)) {
                is Outcome.Success -> _state.value = _state.value.copy(submitting = false, placedCode = r.value.order.code)
                is Outcome.Failure -> _state.value = _state.value.copy(
                    submitting = false,
                    errors = (r.error as? AppError.Validation)?.fields.orEmpty(),
                    generalError = r.error,
                )
            }
        }
    }
}
