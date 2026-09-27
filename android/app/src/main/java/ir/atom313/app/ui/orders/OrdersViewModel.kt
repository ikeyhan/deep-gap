package ir.atom313.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.data.paging.pager
import ir.atom313.app.data.repository.OrderRepository
import ir.atom313.app.domain.model.MyProduct
import ir.atom313.app.domain.model.MyProductsFilter
import ir.atom313.app.domain.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/** «محصولات من» — اقلام خریداری‌شده با فیلتر وضعیت. */
@HiltViewModel
class MyProductsViewModel @Inject constructor(
    private val orders: OrderRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(MyProductsFilter.ALL)
    val filter: StateFlow<MyProductsFilter> = _filter.asStateFlow()

    val items: Flow<PagingData<MyProduct>> = _filter
        .flatMapLatest { f -> pager(pageSize = 20) { page, limit -> orders.myProducts(page, f, limit) } }
        .cachedIn(viewModelScope)

    fun setFilter(filter: MyProductsFilter) { _filter.value = filter }
}

/** فهرست سفارش‌ها. */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orders: OrderRepository,
) : ViewModel() {
    val items: Flow<PagingData<Order>> =
        pager(pageSize = 20) { page, limit -> orders.orders(page, limit) }.cachedIn(viewModelScope)
}
