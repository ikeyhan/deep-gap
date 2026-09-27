package ir.atom313.app.ui.catalog

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.RatingBadge
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.data.paging.toAppError
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.LocalWindowWidth

private enum class ExploreTab(val label: String) { PRODUCTS("محصولات"), SELLERS("فروشگاه‌ها"), OFFICES("دفاتر محلات") }

/**
 * صفحهٔ کاوش — کاتالوگ سایت در قالب موبایل: جستجوی یکپارچه، تب‌های محصول/فروشگاه/دفتر،
 * دسته‌بندی‌های افقی و فیلترها در شیت پایین (به‌جای ستون فیلتر دسکتاپ).
 */
@Composable
fun ExploreScreen(
    onProduct: (Long) -> Unit,
    onSeller: (Long) -> Unit,
    onOffice: (Long) -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    initialQuery: String = "",
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val wished by viewModel.wishedIds.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()
    val sellers = viewModel.sellers.collectAsLazyPagingItems()
    val offices = viewModel.offices.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && query.isBlank()) viewModel.setQuery(initialQuery)
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        onSearch = { viewModel.commitSearch(); focusManager.clearFocus() },
                        modifier = Modifier.weight(1f),
                    )
                    if (tab == 0) {
                        IconButton(onClick = { showFilters = true }) {
                            BadgedBox(badge = { if (filter.activeCount > 0) Badge { Text(Formatters.number(filter.activeCount.toLong())) } }) {
                                Icon(Icons.Outlined.FilterList, contentDescription = "فیلترها")
                            }
                        }
                    }
                }
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    ExploreTab.entries.forEachIndexed { index, t ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(t.label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                        )
                    }
                }
            }
        }

        when (ExploreTab.entries[tab]) {
            ExploreTab.PRODUCTS -> ProductGrid(
                items = products,
                wished = wished,
                onProduct = onProduct,
                onToggleWish = { viewModel.toggleWish(it, onMessage) },
                onAddToCart = { viewModel.addToCart(it, onMessage) },
                columns = gridColumnsFor(LocalWindowWidth.current),
                contentPadding = contentPadding,
                header = {
                    if (categories.isNotEmpty()) {
                        CategoryFilterRow(
                            categories = categories.map { it.name },
                            selected = filter.category,
                            onSelect = viewModel::setCategory,
                        )
                    }
                },
            )
            ExploreTab.SELLERS -> SellerList(sellers, onSeller, contentPadding)
            ExploreTab.OFFICES -> OfficeList(offices, onOffice, contentPadding)
        }
    }

    if (showFilters) {
        FilterSheet(
            filter = filter,
            categories = categories.map { it.name },
            onApply = { viewModel.applyFilter(it); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
}

/** صفحهٔ جستجوی اختصاصی با تاریخچه — از نوار جستجوی خانه باز می‌شود. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProduct: (Long) -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    initialQuery: String = "",
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val wished by viewModel.wishedIds.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) viewModel.setQuery(initialQuery) else focus.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.sm, end = Spacing.screen, top = Spacing.sm, bottom = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "بازگشت")
                }
                SearchField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    onSearch = { viewModel.commitSearch(); focusManager.clearFocus() },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
            }
        }
        if (query.isBlank()) {
            RecentSearches(recent, onPick = { viewModel.setQuery(it) }, onClear = viewModel::clearRecentSearches)
        } else {
            ProductGrid(
                items = products,
                wished = wished,
                onProduct = onProduct,
                onToggleWish = { viewModel.toggleWish(it, onMessage) },
                onAddToCart = { viewModel.addToCart(it, onMessage) },
                columns = gridColumnsFor(LocalWindowWidth.current),
                contentPadding = contentPadding,
                emptyTitle = "نتیجه‌ای یافت نشد",
                emptyMessage = "عبارت دیگری را امتحان کنید یا از دسته‌بندی‌ها استفاده کنید.",
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    AtomTextField(
        value = value,
        onValueChange = onValueChange,
        label = "جستجو",
        placeholder = "نام محصول، دسته یا فروشگاه",
        kind = FieldKind.TEXT,
        leadingIcon = Icons.Outlined.Search,
        imeAction = androidx.compose.ui.text.input.ImeAction.Search,
        onImeAction = onSearch,
        maxLength = 80,
        modifier = modifier,
    )
}

@Composable
private fun RecentSearches(items: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Search,
            title = "چه چیزی لازم دارید؟",
            message = "نام محصول، دسته‌بندی یا فروشگاه را بنویسید تا نتایج را ببینید.",
        )
        return
    }
    Column(Modifier.fillMaxWidth().padding(Spacing.screen)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("جستجوهای اخیر", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "پاک کردن",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(Radius.xs)).clickable(onClick = onClear).padding(6.dp),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        items.forEach { q ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).clickable { onPick(q) }.padding(vertical = Spacing.md, horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = AtomTheme.colors.textTertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(q, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(bottom = Spacing.xs)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("همه") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AtomTheme.colors.brandSoft, selectedLabelColor = AtomTheme.colors.brandOnSoft),
            )
        }
        items(categories, key = { it }) { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = { Text(c, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AtomTheme.colors.brandSoft, selectedLabelColor = AtomTheme.colors.brandOnSoft),
            )
        }
    }
}

@Composable
private fun SellerList(items: LazyPagingItems<Seller>, onSeller: (Long) -> Unit, contentPadding: PaddingValues) {
    PagedList(
        items = items,
        contentPadding = contentPadding,
        emptyIcon = Icons.Outlined.Storefront,
        emptyTitle = "فروشگاهی یافت نشد",
        key = { it.id },
    ) { seller ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
                .clickable { onSeller(seller.id) }.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape)) {
                AtomImage(seller.avatar, seller.name, Modifier.fillMaxSize(), seed = seller.id, placeholderIconSize = 24.dp)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(seller.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(seller.category, seller.city).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                RatingBadge(seller.rating, seller.sales.coerceAtLeast(1))
            }
        }
    }
}

@Composable
private fun OfficeList(items: LazyPagingItems<Office>, onOffice: (Long) -> Unit, contentPadding: PaddingValues) {
    PagedList(
        items = items,
        contentPadding = contentPadding,
        emptyIcon = Icons.Outlined.LocationCity,
        emptyTitle = "دفتری یافت نشد",
        key = { it.id },
    ) { office ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
                .clickable { onOffice(office.id) }.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(office.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(office.area, office.city).filter { it.isNotBlank() }.joinToString("، "),
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (office.manager.isNotBlank()) {
                    Text("مسئول: ${office.manager}", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1)
                }
            }
        }
    }
}

/** فهرست عمودی صفحه‌بندی‌شده با اسکلتون، خطا و حالت خالی — مشترک فروشگاه‌ها و دفاتر. */
@Composable
private fun <T : Any> PagedList(
    items: LazyPagingItems<T>,
    contentPadding: PaddingValues,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
) {
    val refresh = items.loadState.refresh
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> items(6) { ListItemSkeleton() }
            refresh is LoadState.Error && items.itemCount == 0 -> item { ErrorState(refresh.error.toAppError(), onRetry = items::retry) }
            items.itemCount == 0 && refresh is LoadState.NotLoading -> item {
                EmptyState(emptyIcon, emptyTitle, message = "عبارت جستجو را تغییر دهید.")
            }
        }
        items(items.itemCount, key = items.itemKey { key(it) }) { index ->
            items[index]?.let { row(it) } ?: ListItemSkeleton()
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
