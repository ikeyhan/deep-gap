package ir.atom313.app.ui.cart

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.QuantityStepper
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.CartLine
import ir.atom313.app.domain.model.CartQuote
import ir.atom313.app.ui.component.AtomTopBar

/**
 * سبد خرید — اقلام، کد تخفیف و صورت‌حساب سرور. همهٔ مبالغ از پاسخ سرور می‌آیند
 * تا با سایت یکسان باشند و دست‌کاری سمت دستگاه اثری نداشته باشد.
 */
@Composable
fun CartScreen(
    onProduct: (Long) -> Unit,
    onExplore: () -> Unit,
    onCheckout: (String?) -> Unit,
    contentPadding: PaddingValues,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(
            title = "سبد خرید",
            actions = {
                if (state.lines.isNotEmpty()) {
                    androidx.compose.material3.TextButton(onClick = viewModel::clear) {
                        Text("خالی کردن", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.danger)
                    }
                }
            },
        )

        if (state.lines.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "سبد خرید شما خالی است",
                message = "محصولات موردعلاقه‌تان را به سبد اضافه کنید تا اینجا ببینید.",
                actionLabel = "مشاهدهٔ محصولات",
                onAction = onExplore,
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.md, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.lines, key = { it.productId }) { line ->
                    val problem = state.quote?.lines?.firstOrNull { it.productId == line.productId }
                    CartRow(
                        line = line,
                        serverPrice = problem?.price,
                        unavailable = problem != null && !problem.available,
                        outOfStock = problem != null && problem.available && !problem.inStock,
                        availableStock = problem?.stock ?: 0,
                        onQty = { viewModel.setQty(line.productId, it) },
                        onClick = { onProduct(line.productId) },
                    )
                }

                item(key = "coupon") {
                    Spacer(Modifier.height(Spacing.sm))
                    CouponBox(
                        code = state.coupon,
                        applied = state.quote?.coupon?.code,
                        error = state.quote?.couponError,
                        onChange = viewModel::setCoupon,
                        onApply = viewModel::applyCoupon,
                        onRemove = viewModel::removeCoupon,
                    )
                }

                if (state.quote != null) {
                    item(key = "summary") {
                        Spacer(Modifier.height(Spacing.sm))
                        QuoteSummary(state.quote!!)
                    }
                }
            }
        }

        CheckoutBar(
            quote = state.quote,
            loading = state.quoting,
            error = state.error?.message,
            onRetry = viewModel::retry,
            onCheckout = { onCheckout(viewModel.validCoupon()) },
            bottomPadding = contentPadding.calculateBottomPadding(),
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    serverPrice: Long?,
    unavailable: Boolean,
    outOfStock: Boolean,
    availableStock: Int,
    onQty: (Int) -> Unit,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(Radius.sm)).clickable(onClick = onClick)) {
                AtomImage(line.image, line.title, Modifier.fillMaxSize(), seed = line.productId, placeholderIconSize = 26.dp)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    line.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onClick),
                )
                if (line.seller.isNotBlank()) {
                    Text(line.seller, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1)
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceText(( serverPrice ?: line.localPrice) * line.qty, Modifier.weight(1f), emphasized = false)
                    QuantityStepper(line.qty, onQty, max = if (availableStock > 0) minOf(availableStock, 99) else 99)
                }
            }
        }
        val warning = when {
            unavailable -> "این محصول دیگر در دسترس نیست؛ آن را حذف کنید."
            outOfStock -> if (availableStock > 0) "موجودی کافی نیست (موجود: ${Formatters.number(availableStock.toLong())})" else "این محصول فعلاً ناموجود است."
            else -> null
        }
        AnimatedVisibility(visible = warning != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.sm).clip(RoundedCornerShape(Radius.xs))
                    .background(AtomTheme.colors.warningSoft).padding(horizontal = Spacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = AtomTheme.colors.warning, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(warning.orEmpty(), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.warning)
            }
        }
    }
}

@Composable
private fun CouponBox(
    code: String,
    applied: String?,
    error: String?,
    onChange: (String) -> Unit,
    onApply: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("کد تخفیف", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(Spacing.sm))
        if (applied != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.successSoft)
                    .padding(start = Spacing.md, end = Spacing.xs, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("کد «$applied» اعمال شد", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.success, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onRemove) {
                    Text("حذف", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.danger)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                AtomTextField(
                    value = code, onValueChange = onChange, label = "کد تخفیف",
                    kind = FieldKind.USERNAME, error = error, maxLength = 40,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done, onImeAction = onApply,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                PrimaryButton("اعمال", onApply, enabled = code.isNotBlank(), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun QuoteSummary(quote: CartQuote) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
        SummaryRow("جمع کالاها", Formatters.toman(quote.subtotal))
        if (quote.discount > 0) SummaryRow("تخفیف", "− " + Formatters.toman(quote.discount), color = AtomTheme.colors.danger)
        SummaryRow("هزینهٔ ارسال", if (quote.shipping == 0L) "رایگان" else Formatters.toman(quote.shipping))
        if (quote.toFreeShipping > 0) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "${Formatters.toman(quote.toFreeShipping)} تا ارسال رایگان باقی مانده",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (quote.freeShippingMin == 0L) 0f else ((quote.subtotal - quote.discount).toFloat() / quote.freeShippingMin).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(Radius.pill)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = AtomTheme.colors.surface2,
                drawStopIndicator = {},
            )
        }
        if (quote.belowMinimum) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "حداقل مبلغ سفارش ${Formatters.toman(quote.minOrder)} است.",
                style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.warning,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color ?: MaterialTheme.colorScheme.onSurface)
    }
}

/** نوار پایین سبد: مبلغ قابل پرداخت و دکمهٔ ادامه. */
@Composable
private fun CheckoutBar(
    quote: CartQuote?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCheckout: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Column(Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.md, bottom = bottomPadding + Spacing.md)) {
            if (error != null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Spacing.sm).clip(RoundedCornerShape(Radius.sm))
                        .background(AtomTheme.colors.dangerSoft).padding(horizontal = Spacing.sm, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(error, style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.danger, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onRetry) {
                        Text("تلاش دوباره", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("قابل پرداخت", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                    if (quote == null && loading) {
                        Text("در حال محاسبه…", style = MaterialTheme.typography.titleSmall, color = AtomTheme.colors.textTertiary)
                    } else {
                        PriceText(quote?.total ?: 0)
                    }
                }
                PrimaryButton(
                    text = "ادامهٔ خرید",
                    onClick = onCheckout,
                    loading = loading && quote == null,
                    enabled = quote != null && !quote.hasProblems && !quote.belowMinimum,
                )
            }
        }
    }
}
