package ir.atom313.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.getOrNull
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.data.repository.CatalogRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.Category
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductFilter
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.ui.cart.CartActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * کاتالوگ محصولات با فیلتر و جستجو. تغییر متن جستجو با تأخیر ۳۵۰ms به سرور می‌رود
 * تا با هر حرف درخواست تازه ساخته نشود.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val wishlist: WishlistRepository,
    private val cartActions: CartActions,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _filter = MutableStateFlow(ProductFilter())
    val filter: StateFlow<ProductFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    /** متن درون فیلد جستجو (بدون تأخیر، برای نمایش فوری). */
    val query: StateFlow<String> = _query.asStateFlow()

    val categories: StateFlow<List<Category>> = MutableStateFlow<List<Category>>(emptyList()).also { flow ->
        viewModelScope.launch { flow.value = catalog.categories().getOrNull().orEmpty() }
    }.asStateFlow()

    val recentSearches: StateFlow<List<String>> =
        prefs.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: Flow<PagingData<Product>> = _filter
        .debounce { if (it.query.isEmpty()) 0L else 350L }
        .distinctUntilChanged()
        .flatMapLatest { catalog.products(it) }
        .cachedIn(viewModelScope)

    private val sellerQuery = MutableStateFlow("")
    val sellers: Flow<PagingData<Seller>> = sellerQuery.debounce(350).distinctUntilChanged()
        .flatMapLatest { catalog.sellers(it) }.cachedIn(viewModelScope)

    private val officeQuery = MutableStateFlow("")
    val offices: Flow<PagingData<Office>> = officeQuery.debounce(350).distinctUntilChanged()
        .flatMapLatest { catalog.offices(it) }.cachedIn(viewModelScope)

    val wishedIds: StateFlow<Set<Long>> = wishlist.ids

    fun setQuery(q: String) {
        _query.value = q
        _filter.value = _filter.value.copy(query = q)
        sellerQuery.value = q
        officeQuery.value = q
    }

    /** ثبت جستجوی انجام‌شده در تاریخچه (هنگام زدن دکمهٔ جستجو). */
    fun commitSearch() {
        val q = _query.value.trim()
        if (q.length >= 2) viewModelScope.launch { prefs.addRecentSearch(q) }
    }

    fun clearRecentSearches() = viewModelScope.launch { prefs.clearRecentSearches() }

    fun applyFilter(filter: ProductFilter) { _filter.value = filter.copy(query = _query.value) }
    fun setCategory(category: String?) { _filter.value = _filter.value.copy(category = category) }

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
