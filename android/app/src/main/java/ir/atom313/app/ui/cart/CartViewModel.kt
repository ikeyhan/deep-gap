package ir.atom313.app.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.CartRepository
import ir.atom313.app.domain.model.CartLine
import ir.atom313.app.domain.model.CartQuote
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val quote: CartQuote? = null,
    val quoting: Boolean = false,
    val error: AppError? = null,
    val coupon: String = "",
    val couponApplied: String? = null,
)

/**
 * سبد خرید. مبالغ هرگز روی دستگاه محاسبه نمی‌شوند: هر تغییر در اقلام یا کد تخفیف،
 * پیش‌فاکتور تازه از سرور می‌گیرد (با تأخیر کوتاه تا با هر لمس درخواست نرود).
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
) : ViewModel() {

    private val _coupon = MutableStateFlow("")
    private val _appliedCoupon = MutableStateFlow<String?>(null)
    private val _quote = MutableStateFlow<CartQuote?>(null)
    private val _quoting = MutableStateFlow(false)
    private val _error = MutableStateFlow<AppError?>(null)

    val state: StateFlow<CartUiState> = combine(
        cart.lines, _quote, _quoting, _error, combine(_coupon, _appliedCoupon) { a, b -> a to b },
    ) { lines, quote, quoting, error, (coupon, applied) ->
        CartUiState(lines, quote, quoting, error, coupon, applied)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        viewModelScope.launch {
            combine(cart.lines, _appliedCoupon) { lines, coupon -> lines to coupon }
                .debounce(250)
                .distinctUntilChanged()
                .collect { (lines, coupon) -> refreshQuote(lines, coupon) }
        }
    }

    private suspend fun refreshQuote(lines: List<CartLine>, coupon: String?) {
        if (lines.isEmpty()) { _quote.value = null; _error.value = null; return }
        _quoting.value = true
        when (val r = cart.quote(lines, coupon)) {
            is Outcome.Success -> { _quote.value = r.value; _error.value = null }
            is Outcome.Failure -> _error.value = r.error
        }
        _quoting.value = false
    }

    fun retry() = viewModelScope.launch { refreshQuote(cart.snapshot(), _appliedCoupon.value) }

    fun setQty(productId: Long, qty: Int) = viewModelScope.launch { cart.setQty(productId, qty) }
    fun remove(productId: Long) = viewModelScope.launch { cart.remove(productId) }
    fun clear() = viewModelScope.launch { cart.clear() }

    fun setCoupon(code: String) { _coupon.value = code }
    fun applyCoupon() { _appliedCoupon.value = _coupon.value.trim().ifBlank { null } }
    fun removeCoupon() { _coupon.value = ""; _appliedCoupon.value = null }

    /** کد تخفیف تأییدشده برای مرحلهٔ پرداخت. */
    fun validCoupon(): String? = _quote.value?.coupon?.code
}
