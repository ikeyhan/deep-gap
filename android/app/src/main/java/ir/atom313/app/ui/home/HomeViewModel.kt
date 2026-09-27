package ir.atom313.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.CatalogRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.Home
import ir.atom313.app.domain.model.Product
import ir.atom313.app.ui.cart.CartActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val wishlist: WishlistRepository,
    private val cartActions: CartActions,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val state: StateFlow<LoadState<Home>> = refreshTrigger
        .flatMapLatest { catalog.home() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    val wishedIds: StateFlow<Set<Long>> = wishlist.ids

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            refreshTrigger.value++
            // نشانگر کشیدن تا دریافت نتیجه باقی می‌ماند
            kotlinx.coroutines.delay(400)
            _refreshing.value = false
        }
    }

    fun toggleWish(productId: Long, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = wishlist.toggle(productId)) {
                is Outcome.Success -> onMessage(if (r.value) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد")
                is Outcome.Failure -> onMessage(r.error.message)
            }
        }
    }

    fun addToCart(product: Product, onMessage: (String) -> Unit) {
        viewModelScope.launch { cartActions.add(product, onMessage) }
    }
}
