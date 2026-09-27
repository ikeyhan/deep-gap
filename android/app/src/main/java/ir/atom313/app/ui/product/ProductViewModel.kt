package ir.atom313.app.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.CatalogRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductDetail
import ir.atom313.app.ui.cart.CartActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewFormState(
    val body: String = "",
    val rating: Int = 5,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val wishlist: WishlistRepository,
    private val cartActions: CartActions,
    private val session: SessionRepository,
) : ViewModel() {

    val productId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val reload = MutableStateFlow(0)
    val state: StateFlow<LoadState<ProductDetail>> = reload
        .flatMapLatest { catalog.product(productId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    val wishedIds: StateFlow<Set<Long>> = wishlist.ids

    private val _reviewForm = MutableStateFlow(ReviewFormState())
    val reviewForm: StateFlow<ReviewFormState> = _reviewForm.asStateFlow()

    val isSignedIn: Boolean get() = session.isSignedIn

    fun retry() { reload.value++ }

    fun toggleWish(onMessage: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = wishlist.toggle(productId)) {
                is Outcome.Success -> onMessage(if (r.value) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد")
                is Outcome.Failure -> onMessage(r.error.message)
            }
        }
    }

    fun addToCart(product: Product, qty: Int, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            repeat(qty - 1) { cartActions.add(product) {} }
            cartActions.add(product, onMessage)
        }
    }

    fun setReviewBody(text: String) { _reviewForm.value = _reviewForm.value.copy(body = text, error = null) }
    fun setReviewRating(rating: Int) { _reviewForm.value = _reviewForm.value.copy(rating = rating) }

    fun submitReview() {
        val form = _reviewForm.value
        if (form.body.trim().length < 5) {
            _reviewForm.value = form.copy(error = "متن نظر را کامل‌تر بنویسید (حداقل ۵ نویسه).")
            return
        }
        viewModelScope.launch {
            _reviewForm.value = form.copy(submitting = true, error = null)
            when (val r = catalog.addReview(productId, form.body, form.rating)) {
                is Outcome.Success -> _reviewForm.value = ReviewFormState(submitted = true)
                is Outcome.Failure -> _reviewForm.value = form.copy(
                    submitting = false,
                    error = (r.error as? AppError.Validation)?.fields?.get("body") ?: r.error.message,
                )
            }
        }
    }
}
