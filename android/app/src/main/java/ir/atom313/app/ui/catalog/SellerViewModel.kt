package ir.atom313.app.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.CatalogRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductFilter
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.ui.cart.CartActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val wishlist: WishlistRepository,
    private val cartActions: CartActions,
) : ViewModel() {

    private val sellerId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L
    private val reload = MutableStateFlow(0)

    val seller: StateFlow<LoadState<Seller>> = reload
        .flatMapLatest { catalog.seller(sellerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    val products: Flow<PagingData<Product>> =
        catalog.products(ProductFilter(sellerId = sellerId)).cachedIn(viewModelScope)

    val wishedIds: StateFlow<Set<Long>> = wishlist.ids

    fun retry() { reload.value++ }

    fun toggleWish(productId: Long, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = wishlist.toggle(productId)) {
                is Outcome.Success -> onMessage(if (r.value) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد")
                is Outcome.Failure -> onMessage(r.error.message)
            }
        }
    }

    fun addToCart(product: Product, onMessage: (String) -> Unit) = viewModelScope.launch { cartActions.add(product, onMessage) }
}
