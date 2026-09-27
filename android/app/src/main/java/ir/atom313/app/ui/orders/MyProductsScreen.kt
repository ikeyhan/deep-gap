package ir.atom313.app.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.StatusChip
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.data.paging.toAppError
import ir.atom313.app.domain.model.MyProduct
import ir.atom313.app.domain.model.MyProductsFilter
import ir.atom313.app.ui.component.AtomTopBar

/**
 * «محصولات من» — هر قلم خریداری‌شده با وضعیت، تاریخ خرید و دسترسی به سفارش.
 * برای مهمان، حالت راهنما با دکمهٔ ورود و پیگیری سفارش نمایش داده می‌شود.
 */
@Composable
fun MyProductsScreen(
    isSignedIn: Boolean,
    onLogin: () -> Unit,
    onTrack: () -> Unit,
    onProduct: (Long) -> Unit,
    onOrder: (String) -> Unit,
    onExplore: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: MyProductsViewModel = hiltViewModel(),
) {
    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "محصولات من")

        if (!isSignedIn) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                EmptyState(
                    icon = Icons.Outlined.Login,
                    title = "برای دیدن خریدهایتان وارد شوید",
                    message = "با حساب سایت اتم ۳۱۳ وارد شوید تا محصولات و سفارش‌هایتان اینجا نمایش داده شود.",
                    actionLabel = "ورود به حساب",
                    onAction = onLogin,
                )
                androidx.compose.material3.TextButton(onClick = onTrack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("پیگیری سفارش بدون ورود")
                }
            }
            return@Column
        }

        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val items = viewModel.items.collectAsLazyPagingItems()
        val refresh = items.loadState.refresh

        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(MyProductsFilter.entries.toList(), key = { it.name }) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AtomTheme.colors.brandSoft,
                        selectedLabelColor = AtomTheme.colors.brandOnSoft,
                    ),
                )
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen, end = Spacing.screen, top = Spacing.xs,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> items(5) { ListItemSkeleton() }
                refresh is LoadState.Error && items.itemCount == 0 -> item {
                    ErrorState(refresh.error.toAppError(), onRetry = items::retry, onLogin = onLogin)
                }
                items.itemCount == 0 && refresh is LoadState.NotLoading -> item {
                    EmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = if (filter == MyProductsFilter.ALL) "هنوز محصولی نخریده‌اید" else "موردی در این وضعیت نیست",
                        message = if (filter == MyProductsFilter.ALL) {
                            "پس از اولین خرید، محصولات شما با وضعیت سفارش اینجا نمایش داده می‌شود."
                        } else {
                            "فیلتر دیگری را انتخاب کنید."
                        },
                        actionLabel = if (filter == MyProductsFilter.ALL) "مشاهدهٔ محصولات" else null,
                        onAction = if (filter == MyProductsFilter.ALL) onExplore else null,
                    )
                }
            }
            items(items.itemCount, key = items.itemKey { "${it.orderCode}-${it.item.id}" }) { index ->
                items[index]?.let { MyProductRow(it, onProduct, onOrder) } ?: ListItemSkeleton()
            }
            if (items.loadState.append is LoadState.Loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyProductRow(item: MyProduct, onProduct: (Long) -> Unit, onOrder: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
            .clickable { onOrder(item.orderCode) }.padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(Radius.sm))
                    .then(if (item.item.productId != null) Modifier.clickable { onProduct(item.item.productId) } else Modifier),
            ) {
                AtomImage(item.item.image, item.item.title, Modifier.fillMaxSize(), seed = item.item.id, placeholderIconSize = 24.dp)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(item.item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.item.seller.isNotBlank()) {
                    Text(item.item.seller, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                StatusChip(item.item.status)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "سفارش #${item.orderCode}" + if (item.item.qty > 1) " · ${Formatters.number(item.item.qty.toLong())} عدد" else "",
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                )
                Text(
                    "تاریخ خرید: ${Formatters.persianDate(item.purchasedAt)}",
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                )
            }
            PriceText(item.item.amount, emphasized = false)
        }
    }
}
