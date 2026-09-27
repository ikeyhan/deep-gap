package ir.atom313.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.ProductCardSkeleton
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.data.paging.toAppError
import ir.atom313.app.domain.model.Product

/**
 * شبکهٔ محصولات با صفحه‌بندی تنبل. تعداد ستون‌ها بر اساس عرض صفحه تعیین می‌شود
 * (گوشی کوچک ۲، تبلت و تاشو ۳ یا ۴) — بدون چیدمان ثابت پیکسلی.
 */
@Composable
fun ProductGrid(
    items: LazyPagingItems<Product>,
    wished: Set<Long>,
    onProduct: (Long) -> Unit,
    onToggleWish: (Long) -> Unit,
    onAddToCart: (Product) -> Unit,
    columns: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    emptyTitle: String = "محصولی یافت نشد",
    emptyMessage: String? = "فیلترها را تغییر دهید یا عبارت دیگری جستجو کنید.",
    header: (@Composable () -> Unit)? = null,
) {
    val refresh = items.loadState.refresh
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen, end = Spacing.screen,
            top = Spacing.md, bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        when {
            refresh is LoadState.Loading && items.itemCount == 0 ->
                items(6) { ProductCardSkeleton() }
            refresh is LoadState.Error && items.itemCount == 0 ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorState(refresh.error.toAppError(), onRetry = items::retry)
                }
            items.itemCount == 0 && refresh is LoadState.NotLoading ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(Icons.Outlined.SearchOff, emptyTitle, emptyMessage)
                }
        }
        items(items.itemCount, key = items.itemKey { it.id }) { index ->
            val product = items[index]
            if (product != null) {
                ProductCard(
                    product = product,
                    wished = product.id in wished,
                    onClick = { onProduct(product.id) },
                    onToggleWish = { onToggleWish(product.id) },
                    onAddToCart = { onAddToCart(product) },
                )
            } else {
                ProductCardSkeleton()
            }
        }
        if (items.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.5.dp) }
            }
        }
        if (items.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorState((items.loadState.append as LoadState.Error).error.toAppError(), onRetry = items::retry)
            }
        }
    }
}

/** تعداد ستون مناسب برای عرض فعلی صفحه. */
fun gridColumnsFor(width: Dp): Int = when {
    width < 380.dp -> 2
    width < 600.dp -> 2
    width < 840.dp -> 3
    else -> 4
}
