package ir.atom313.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.LogoMark
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.ProductCardSkeleton
import ir.atom313.app.core.designsystem.component.RatingBadge
import ir.atom313.app.core.designsystem.component.SectionHeader
import ir.atom313.app.core.designsystem.component.SkeletonBox
import ir.atom313.app.core.designsystem.component.StaleBanner
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.Article
import ir.atom313.app.domain.model.Category
import ir.atom313.app.domain.model.Home
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.domain.model.Slide
import kotlinx.coroutines.delay

/**
 * صفحهٔ خانه — همان بخش‌های صفحهٔ اول سایت (اسلایدر، دسته‌ها، جدیدترین‌ها،
 * پرفروش‌ها، فروشگاه‌ها، دفاتر محلات، بلاگ) اما با چیدمان موبایل و بارگذاری تنبل.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    unreadCount: Int,
    onProduct: (Long) -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onSeller: (Long) -> Unit,
    onOffice: (Long) -> Unit,
    onArticle: (Long) -> Unit,
    onAllProducts: () -> Unit,
    onAllSellers: () -> Unit,
    onAllOffices: () -> Unit,
    onAllArticles: () -> Unit,
    onNotifications: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wished by viewModel.wishedIds.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        HomeHeader(unreadCount, onSearch, onNotifications)
        StaleBanner(state.error.takeIf { state.data != null }, viewModel::refresh)
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                state.isInitialLoading -> HomeSkeleton(contentPadding)
                state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.fillMaxSize())
                state.data != null -> HomeContent(
                    home = state.data!!,
                    wished = wished,
                    contentPadding = contentPadding,
                    onProduct = onProduct,
                    onCategory = onCategory,
                    onSeller = onSeller,
                    onOffice = onOffice,
                    onArticle = onArticle,
                    onAllProducts = onAllProducts,
                    onAllSellers = onAllSellers,
                    onAllOffices = onAllOffices,
                    onAllArticles = onAllArticles,
                    onToggleWish = { viewModel.toggleWish(it, onMessage) },
                    onAddToCart = { viewModel.addToCart(it, onMessage) },
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(unreadCount: Int, onSearch: () -> Unit, onNotifications: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoMark(Modifier.size(32.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("اتم ۳۱۳", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onNotifications) {
                    BadgedBox(badge = { if (unreadCount > 0) Badge { Text(Formatters.number(unreadCount.toLong())) } }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "اعلان‌ها")
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            // نوار جستجو (مانند هدر سایت) — لمس آن صفحهٔ جستجو را باز می‌کند
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.surface2)
                    .clickable(onClick = onSearch).padding(horizontal = Spacing.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = AtomTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("جستجو در محصولات و خدمات…", style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textTertiary)
            }
        }
    }
}

@Composable
internal fun HomeContent(
    home: Home,
    wished: Set<Long>,
    contentPadding: PaddingValues,
    onProduct: (Long) -> Unit,
    onCategory: (String) -> Unit,
    onSeller: (Long) -> Unit,
    onOffice: (Long) -> Unit,
    onArticle: (Long) -> Unit,
    onAllProducts: () -> Unit,
    onAllSellers: () -> Unit,
    onAllOffices: () -> Unit,
    onAllArticles: () -> Unit,
    onToggleWish: (Long) -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.md, bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        if (home.slides.isNotEmpty()) {
            item(key = "slides") { SlidesPager(home.slides, onAllProducts) }
        }
        if (home.announcement != null) {
            item(key = "ann") {
                Row(
                    Modifier.padding(horizontal = Spacing.screen).fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.brandSoft).padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        if (home.announcement.title.isNotBlank()) {
                            Text(home.announcement.title, style = MaterialTheme.typography.titleSmall, color = AtomTheme.colors.brandOnSoft)
                        }
                        if (home.announcement.text.isNotBlank()) {
                            Text(home.announcement.text, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.brandOnSoft)
                        }
                    }
                }
            }
        }
        if (home.categories.isNotEmpty()) {
            item(key = "cats") {
                Column {
                    SectionHeader("دسته‌بندی‌ها", actionLabel = "همه", onAction = onAllProducts)
                    Spacer(Modifier.height(Spacing.md))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(home.categories, key = { it.name }) { CategoryChip(it) { onCategory(it.name) } }
                    }
                }
            }
        }
        if (home.newest.isNotEmpty()) {
            item(key = "newest") {
                ProductRow("جدیدترین محصولات", home.newest, wished, onAllProducts, onProduct, onToggleWish, onAddToCart)
            }
        }
        if (home.popular.isNotEmpty()) {
            item(key = "popular") {
                ProductRow("پرفروش‌ترین‌ها", home.popular, wished, onAllProducts, onProduct, onToggleWish, onAddToCart)
            }
        }
        if (home.sellers.isNotEmpty()) {
            item(key = "sellers") {
                Column {
                    SectionHeader("فروشگاه‌ها", actionLabel = "همه", onAction = onAllSellers)
                    Spacer(Modifier.height(Spacing.md))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(home.sellers, key = { it.id }) { SellerChipCard(it) { onSeller(it.id) } }
                    }
                }
            }
        }
        if (home.offices.isNotEmpty()) {
            item(key = "offices") {
                Column {
                    SectionHeader("دفاتر محلات", actionLabel = "همه", onAction = onAllOffices)
                    Spacer(Modifier.height(Spacing.md))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(home.offices, key = { it.id }) { OfficeChipCard(it) { onOffice(it.id) } }
                    }
                }
            }
        }
        if (home.articles.isNotEmpty()) {
            item(key = "articles") {
                Column {
                    SectionHeader("از بلاگ اتم", actionLabel = "همه", onAction = onAllArticles)
                    Spacer(Modifier.height(Spacing.md))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(home.articles, key = { it.id }) { ArticleChipCard(it) { onArticle(it.id) } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidesPager(slides: List<Slide>, onCta: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    // چرخش خودکار ملایم؛ با لمس کاربر متوقف می‌شود
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(5_000)
            if (!pagerState.isScrollInProgress && pagerState.pageCount > 1) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
            }
        }
    }
    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            pageSpacing = Spacing.md,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val slide = slides[page]
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(Radius.lg))
                    .clickable(onClick = onCta),
            ) {
                AtomImage(slide.image, slide.title, Modifier.fillMaxSize(), seed = slide.id, contentScale = ContentScale.Crop)
                Box(
                    Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC0B120D)),
                            startY = 120f,
                        ),
                    ),
                )
                Column(Modifier.align(Alignment.BottomEnd).padding(Spacing.lg)) {
                    if (slide.eyebrow.isNotBlank()) {
                        Text(slide.eyebrow, style = MaterialTheme.typography.labelSmall, color = Color(0xFFBDEBCB))
                    }
                    Text(slide.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (slide.subtitle.isNotBlank()) {
                        Text(slide.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE3EBE5), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (slides.size > 1) {
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(slides.size) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        Modifier.padding(horizontal = 3.dp)
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else AtomTheme.colors.border),
                    )
                }
            }
        }
    }
}

/** رنگ‌های ملایم برای تمایز دسته‌ها؛ از روی نام دسته انتخاب می‌شوند تا همیشه ثابت بمانند. */
private val categoryTints = listOf(
    Color(0xFF149B3E), Color(0xFF0E7A38), Color(0xFFB26A00), Color(0xFF2F8F83),
    Color(0xFF4A7C59), Color(0xFF6B8E23), Color(0xFF2563EB), Color(0xFF8A5A2B),
)

@Composable
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    val base = categoryTints[(category.name.hashCode().mod(categoryTints.size))]
    // در تم تاریک رنگ‌های تیره خوانا نیستند؛ به سفید نزدیک می‌شوند تا کنتراست کافی بماند
    val tint = if (AtomTheme.colors.isDark) lerp(base, Color.White, 0.45f) else base
    Column(
        Modifier.width(84.dp).clip(RoundedCornerShape(Radius.md)).clickable(onClick = onClick).padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(base.copy(alpha = if (AtomTheme.colors.isDark) 0.24f else 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            // حرف اول دستهٔ‌بندی — خواناتر و قابل‌تفکیک‌تر از تکرار یک نماد یکسان
            Text(
                category.name.trim().take(1),
                style = MaterialTheme.typography.titleLarge,
                color = tint,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.name, style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (category.count > 0) {
            Text(
                Formatters.number(category.count.toLong()) + " کالا",
                style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProductRow(
    title: String,
    products: List<Product>,
    wished: Set<Long>,
    onAll: () -> Unit,
    onProduct: (Long) -> Unit,
    onToggleWish: (Long) -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    Column {
        SectionHeader(title, actionLabel = "مشاهدهٔ همه", onAction = onAll)
        Spacer(Modifier.height(Spacing.md))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(products, key = { it.id }) { p ->
                ProductCard(
                    product = p,
                    wished = p.id in wished,
                    onClick = { onProduct(p.id) },
                    onToggleWish = { onToggleWish(p.id) },
                    onAddToCart = { onAddToCart(p) },
                    modifier = Modifier.width(168.dp),
                )
            }
        }
    }
}

@Composable
private fun SellerChipCard(seller: Seller, onClick: () -> Unit) {
    Column(
        Modifier.width(140.dp).clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape)) {
            AtomImage(seller.avatar, seller.name, Modifier.fillMaxSize(), seed = seller.id, placeholderIconSize = 24.dp)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(seller.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(seller.city.ifBlank { seller.category }, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        RatingBadge(seller.rating, seller.sales.coerceAtLeast(1))
    }
}

@Composable
private fun OfficeChipCard(office: Office, onClick: () -> Unit) {
    Row(
        Modifier.width(230.dp).clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text(office.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(office.area, office.city).filter { it.isNotBlank() }.joinToString("، "),
                style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArticleChipCard(article: Article, onClick: () -> Unit) {
    Column(
        Modifier.width(230.dp).clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick),
    ) {
        AtomImage(article.cover, article.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f), seed = article.id)
        Column(Modifier.padding(Spacing.md)) {
            Text(article.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(
                Formatters.persianDate(article.createdAt), style = MaterialTheme.typography.labelSmall,
                color = AtomTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun HomeSkeleton(contentPadding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(top = Spacing.md, bottom = contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        SkeletonBox(Modifier.padding(horizontal = Spacing.screen).fillMaxWidth().aspectRatio(16f / 9f), Radius.lg)
        Row(Modifier.padding(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SkeletonBox(Modifier.size(56.dp), 28.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.width(56.dp).height(10.dp))
                }
            }
        }
        Row(Modifier.padding(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(2) { ProductCardSkeleton(Modifier.width(168.dp)) }
        }
    }
}
