package ir.atom313.app.ui.product

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.QuantityStepper
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.component.SectionHeader
import ir.atom313.app.core.designsystem.component.SkeletonBox
import ir.atom313.app.core.designsystem.component.StaleBanner
import ir.atom313.app.core.designsystem.component.StarRow
import ir.atom313.app.core.designsystem.component.WishButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductDetail
import ir.atom313.app.domain.model.Review
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.shareText

/**
 * جزئیات محصول — بازطراحی‌شده برای موبایل: تصویر بزرگ، اطلاعات فروشنده،
 * توضیحات، نظرات و نوار خرید چسبیده به پایین (به‌جای چیدمان دوستونی سایت).
 */
@Composable
fun ProductScreen(
    onBack: () -> Unit,
    onSeller: (Long) -> Unit,
    onProduct: (Long) -> Unit,
    onCart: () -> Unit,
    onLogin: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wished by viewModel.wishedIds.collectAsStateWithLifecycle()
    val reviewForm by viewModel.reviewForm.collectAsStateWithLifecycle()
    var qty by rememberSaveable { mutableIntStateOf(1) }
    val context = LocalContext.current
    val detail = state.data

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(
            title = detail?.product?.title ?: "محصول",
            onBack = onBack,
            actions = {
                if (detail != null) {
                    IconButton(onClick = {
                        shareText(
                            context,
                            "${detail.product.title}\n${Formatters.toman(detail.product.price)}\nاتم ۳۱۳",
                        )
                    }) { Icon(Icons.Outlined.Share, contentDescription = "اشتراک‌گذاری") }
                }
            },
        )
        StaleBanner(state.error.takeIf { detail != null }, viewModel::retry)

        when {
            state.isInitialLoading -> ProductSkeleton()
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::retry, onHome = onBack, modifier = Modifier.fillMaxSize())
            detail != null -> Box(Modifier.fillMaxSize()) {
                ProductContent(
                    detail = detail,
                    wished = detail.product.id in wished,
                    reviewForm = reviewForm,
                    isSignedIn = viewModel.isSignedIn,
                    onToggleWish = { viewModel.toggleWish(onMessage) },
                    onSeller = onSeller,
                    onProduct = onProduct,
                    onReviewBody = viewModel::setReviewBody,
                    onReviewRating = viewModel::setReviewRating,
                    onSubmitReview = viewModel::submitReview,
                    onLogin = onLogin,
                    onAddRelatedToCart = { viewModel.addToCart(it, 1, onMessage) },
                    wishedIds = wished,
                    onToggleWishFor = { viewModel.toggleWish(onMessage) },
                    bottomInset = contentPadding.calculateBottomPadding() + 96.dp,
                )
                BuyBar(
                    product = detail.product,
                    qty = qty,
                    onQty = { qty = it.coerceAtLeast(1) },
                    onAdd = { viewModel.addToCart(detail.product, qty, onMessage) },
                    onCart = onCart,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ProductContent(
    detail: ProductDetail,
    wished: Boolean,
    reviewForm: ReviewFormState,
    isSignedIn: Boolean,
    onToggleWish: () -> Unit,
    onSeller: (Long) -> Unit,
    onProduct: (Long) -> Unit,
    onReviewBody: (String) -> Unit,
    onReviewRating: (Int) -> Unit,
    onSubmitReview: () -> Unit,
    onLogin: () -> Unit,
    onAddRelatedToCart: (Product) -> Unit,
    wishedIds: Set<Long>,
    onToggleWishFor: (Long) -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val p = detail.product
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item(key = "image") {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surface)) {
                AtomImage(p.image, p.title, Modifier.fillMaxSize(), seed = p.id, placeholderIconSize = 72.dp)
                WishButton(wished, onToggleWish, Modifier.align(Alignment.TopStart).padding(Spacing.md))
                if (!p.inStock) {
                    Text(
                        "ناموجود",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md)
                            .clip(RoundedCornerShape(Radius.xs)).background(androidx.compose.ui.graphics.Color(0xCC1A1D1B))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        item(key = "title") {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                if (p.category.isNotBlank()) {
                    Text(p.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                }
                Text(p.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (p.rating != null && p.reviewCount > 0) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AtomTheme.colors.star, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(p.rating), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "(${Formatters.number(p.reviewCount.toLong())} نظر)",
                            style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary,
                        )
                    } else {
                        Text("بدون نظر ثبت‌شده", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
                    }
                    Spacer(Modifier.weight(1f))
                    if (p.inStock && p.stock <= 5) {
                        Text(
                            "تنها ${Formatters.number(p.stock.toLong())} عدد باقی مانده",
                            style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.warning,
                        )
                    }
                }
            }
        }

        if (p.seller.isNotBlank()) {
            item(key = "seller") {
                Row(
                    Modifier.padding(horizontal = Spacing.screen).fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
                        .then(if (p.sellerId != null) Modifier.clickable { onSeller(p.sellerId) } else Modifier)
                        .padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text("فروشنده", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                        Text(p.seller, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (p.sellerId != null) {
                        Text("مشاهدهٔ فروشگاه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item(key = "assurance") {
            Row(
                Modifier.padding(horizontal = Spacing.screen).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AssuranceChip(Icons.Outlined.VerifiedUser, "ضمانت اصالت کالا", Modifier.weight(1f))
                AssuranceChip(Icons.Outlined.LocalShipping, "ارسال به سراسر کشور", Modifier.weight(1f))
            }
        }

        if (p.description.isNotBlank()) {
            item(key = "desc") {
                Column(Modifier.padding(horizontal = Spacing.screen)) {
                    Text("معرفی محصول", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.sm))
                    ExpandableText(p.description)
                }
            }
        }

        item(key = "reviews-header") {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Text("نظرات خریداران", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.md))
                RatingSummary(detail)
            }
        }

        if (detail.reviews.isEmpty()) {
            item(key = "no-reviews") {
                Text(
                    "هنوز نظری برای این محصول ثبت نشده است. اولین نفر باشید!",
                    style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textTertiary,
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                )
            }
        } else {
            items(detail.reviews, key = { "r${it.id}" }) { ReviewRow(it) }
        }

        item(key = "review-form") {
            Column(Modifier.padding(horizontal = Spacing.screen)) {
                Divider(color = AtomTheme.colors.border)
                Spacer(Modifier.height(Spacing.lg))
                when {
                    !isSignedIn -> Column {
                        Text("برای ثبت نظر وارد حساب خود شوید.", style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                        Spacer(Modifier.height(Spacing.md))
                        SecondaryButton("ورود به حساب", onLogin)
                    }
                    reviewForm.submitted -> Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.successSoft).padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = AtomTheme.colors.success, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            "نظر شما ثبت شد و پس از تأیید نمایش داده می‌شود.",
                            style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.success,
                        )
                    }
                    else -> Column {
                        Text("ثبت نظر شما", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { onReviewRating(star) }, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "$star ستاره",
                                        tint = if (star <= reviewForm.rating) AtomTheme.colors.star else AtomTheme.colors.border,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        AtomTextField(
                            value = reviewForm.body,
                            onValueChange = onReviewBody,
                            label = "متن نظر",
                            placeholder = "تجربهٔ خود از این محصول را بنویسید…",
                            kind = FieldKind.MULTILINE,
                            error = reviewForm.error,
                            maxLength = 2000,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        PrimaryButton("ارسال نظر", onSubmitReview, loading = reviewForm.submitting)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "نظر شما پس از بررسی و تأیید نمایش داده می‌شود.",
                            style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                        )
                    }
                }
            }
        }

        if (detail.related.isNotEmpty()) {
            item(key = "related") {
                Column {
                    SectionHeader("محصولات مشابه")
                    Spacer(Modifier.height(Spacing.md))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(detail.related, key = { it.id }) { rel ->
                            ProductCard(
                                product = rel,
                                wished = rel.id in wishedIds,
                                onClick = { onProduct(rel.id) },
                                onToggleWish = { onToggleWishFor(rel.id) },
                                onAddToCart = { onAddRelatedToCart(rel) },
                                modifier = Modifier.width(168.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssuranceChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.surface2).padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

@Composable
private fun ExpandableText(text: String, collapsedLines: Int = 5) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = AtomTheme.colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (text.length > 220) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (expanded) "بستن" else "ادامهٔ توضیحات",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(Radius.xs)).clickable { expanded = !expanded }.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RatingSummary(detail: ProductDetail) {
    val total = detail.ratingDistribution.sum()
    if (total == 0) return
    val average = detail.product.rating ?: 0.0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
            Text("%.1f".format(average), style = MaterialTheme.typography.displaySmall)
            StarRow(average.toInt())
            Text("${Formatters.number(total.toLong())} نظر", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (5 downTo 1).forEach { star ->
                val count = detail.ratingDistribution.getOrElse(star - 1) { 0 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Formatters.number(star.toLong()), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(14.dp))
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AtomTheme.colors.star, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else count.toFloat() / total },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(Radius.pill)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = AtomTheme.colors.surface2,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(Formatters.number(count.toLong()), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, modifier = Modifier.width(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(review: Review) {
    Column(Modifier.padding(horizontal = Spacing.screen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) { Text(review.author.take(1), style = MaterialTheme.typography.titleSmall, color = AtomTheme.colors.brandOnSoft) }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(review.author, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                StarRow(review.rating, 12.dp)
            }
            Text(Formatters.relative(review.createdAt), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(review.body, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
    }
}

/** نوار خرید چسبیده به پایین — قیمت، تعداد و دکمهٔ افزودن همیشه در دسترس. */
@Composable
private fun BuyBar(
    product: Product,
    qty: Int,
    onQty: (Int) -> Unit,
    onAdd: () -> Unit,
    onCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var added by remember { mutableStateOf(false) }
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.screen, vertical = Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("قیمت", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                    PriceText(product.price * qty)
                }
                if (product.inStock) {
                    QuantityStepper(qty, { if (it >= 1) onQty(it) }, max = minOf(product.stock, 99))
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = if (product.inStock) "افزودن به سبد" else "ناموجود",
                    onClick = { onAdd(); added = true },
                    enabled = product.inStock,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(visible = added) {
                    SecondaryButton("سبد خرید", onCart)
                }
            }
        }
    }
}

@Composable
private fun ProductSkeleton() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1f), 0.dp)
        Column(Modifier.padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonBox(Modifier.fillMaxWidth(0.4f).height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.9f).height(22.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(18.dp))
            Spacer(Modifier.height(Spacing.sm))
            SkeletonBox(Modifier.fillMaxWidth().height(72.dp), Radius.md)
        }
    }
}
