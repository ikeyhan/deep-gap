package ir.atom313.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Motion
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.domain.model.Product

/** کارت سطح (سفید با کادر ۱dp و سایهٔ بسیار ملایم — سبک فلت سایت). */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.lg,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(Radius.lg)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, AtomTheme.colors.border)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, elevation = CardDefaults.cardElevation(0.dp)) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border, elevation = CardDefaults.cardElevation(0.dp)) {
            Box(Modifier.padding(contentPadding)) { content() }
        }
    }
}

@Composable
fun PriceText(amount: Long, modifier: Modifier = Modifier, emphasized: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            Formatters.number(amount),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        Text("تومان", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
fun RatingBadge(rating: Double?, count: Int, modifier: Modifier = Modifier) {
    if (rating == null || count == 0) return
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = AtomTheme.colors.star, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            "%.1f".format(rating) + " (" + Formatters.number(count.toLong()) + ")",
            style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textSecondary,
        )
    }
}

@Composable
fun StarRow(rating: Int, size: Dp = 16.dp) {
    Row(Modifier.semantics { contentDescription = "$rating از ۵ ستاره" }) {
        repeat(5) { i ->
            Icon(
                Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(size),
                tint = if (i < rating) AtomTheme.colors.star else AtomTheme.colors.border,
            )
        }
    }
}

/** دکمهٔ قلب با انیمیشن فنری کوتاه. */
@Composable
fun WishButton(active: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, onImage: Boolean = true) {
    val scale by animateFloatAsState(if (active) 1f else 0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "wish")
    val tint by animateColorAsState(if (active) AtomTheme.colors.danger else AtomTheme.colors.textSecondary, tween(Motion.FAST), label = "wishTint")
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (onImage) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f) else Color.Transparent)
            .clickable(role = Role.Checkbox, onClickLabel = if (active) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی", onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (active) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (active) "در علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی",
            tint = tint, modifier = Modifier.size(20.dp).scale(scale),
        )
    }
}

/** کارت محصول شبکه‌ای (مانند .prod-card سایت) — نسخهٔ موبایل با اهداف لمسی بزرگ. */
@Composable
fun ProductCard(
    product: Product,
    wished: Boolean,
    onClick: () -> Unit,
    onToggleWish: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = false) {},
        shape = RoundedCornerShape(Radius.lg),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AtomTheme.colors.border),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg))) {
                AtomImage(product.image, product.title, Modifier.fillMaxSize(), seed = product.id)
                WishButton(wished, onToggleWish, Modifier.align(Alignment.TopStart).padding(Spacing.sm))
                if (!product.inStock) {
                    Text(
                        "ناموجود",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.sm)
                            .clip(RoundedCornerShape(Radius.xs)).background(Color(0xCC1A1D1B)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Column(Modifier.padding(Spacing.md)) {
                if (product.category.isNotBlank()) {
                    Text(product.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.seller, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    RatingBadge(product.rating, product.reviewCount)
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceText(product.price, Modifier.weight(1f))
                    if (product.inStock) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.brandSoft)
                                .clickable(onClickLabel = "افزودن به سبد", role = Role.Button, onClick = onAddToCart),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.AddShoppingCart, contentDescription = "افزودن به سبد", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }
}

/** نشان وضعیت سفارش — رنگ‌های ملایم و قابل‌تشخیص، نه شلوغ. */
@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val c = AtomTheme.colors
    val (fg, bg) = when (status) {
        OrderStatus.DELIVERED -> c.success to c.successSoft
        OrderStatus.SHIPPING -> c.info to c.infoSoft
        OrderStatus.PENDING, OrderStatus.REVIEW -> c.warning to c.warningSoft
        OrderStatus.RETURNED -> c.danger to c.dangerSoft
    }
    Row(
        modifier.clip(RoundedCornerShape(Radius.pill)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(6.dp))
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
fun QuantityStepper(qty: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, max: Int = 99) {
    Row(
        modifier.clip(RoundedCornerShape(Radius.sm)).border(1.dp, AtomTheme.colors.border, RoundedCornerShape(Radius.sm)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(qty + 1) }, enabled = qty < max, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "افزایش تعداد", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(qty.toString(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onChange(qty - 1) }, modifier = Modifier.size(40.dp)) {
            Icon(
                if (qty <= 1) Icons.Outlined.DeleteOutline else Icons.Outlined.Remove,
                contentDescription = if (qty <= 1) "حذف از سبد" else "کاهش تعداد",
                modifier = Modifier.size(18.dp),
                tint = if (qty <= 1) AtomTheme.colors.danger else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** سرتیتر بخش با پیوند «مشاهدهٔ همه». */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(Radius.xs)).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
