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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomImage
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.CenteredLoading
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.component.StaleBanner
import ir.atom313.app.core.designsystem.component.StatusChip
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.data.paging.toAppError
import ir.atom313.app.domain.model.Order
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.ui.component.AtomTopBar

/** فهرست سفارش‌های کاربر. */
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOrder: (String) -> Unit,
    onExplore: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val items = viewModel.items.collectAsLazyPagingItems()
    val refresh = items.loadState.refresh

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "سفارش‌های من", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> items(5) { ListItemSkeleton() }
                refresh is LoadState.Error && items.itemCount == 0 -> item { ErrorState(refresh.error.toAppError(), onRetry = items::retry) }
                items.itemCount == 0 && refresh is LoadState.NotLoading -> item {
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "هنوز سفارشی ثبت نکرده‌اید",
                        message = "اولین خرید خود را از فروشگاه‌های اتم ۳۱۳ انجام دهید.",
                        actionLabel = "شروع خرید",
                        onAction = onExplore,
                    )
                }
            }
            items(items.itemCount, key = items.itemKey { it.code }) { index ->
                items[index]?.let { order -> OrderRow(order) { onOrder(order.code) } } ?: ListItemSkeleton()
            }
            if (items.loadState.append is LoadState.Loading) {
                item { Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.5.dp) } }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("سفارش #${order.code}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusChip(order.status)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            order.items.joinToString("، ") { it.title },
            style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Formatters.persianDate(order.createdAt),
                style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, modifier = Modifier.weight(1f),
            )
            PriceText(order.total, emphasized = false)
        }
    }
}

/** جزئیات سفارش با خط زمانی وضعیت. */
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onProduct: (Long) -> Unit,
    onSupport: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val order = state.data

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(
            title = "سفارش #${viewModel.code}",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(viewModel.code))
                    onMessage("کد سفارش کپی شد")
                }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "کپی کد سفارش") }
            },
        )
        StaleBanner(state.error.takeIf { order != null }, viewModel::retry)
        when {
            state.isInitialLoading -> CenteredLoading()
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::retry, onHome = onBack, modifier = Modifier.fillMaxSize())
            order != null -> OrderDetailContent(order, onProduct, onSupport, contentPadding)
        }
    }
}

@Composable
internal fun OrderDetailContent(order: Order, onProduct: (Long) -> Unit, onSupport: () -> Unit, contentPadding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.md)
            .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Formatters.persianDate(order.createdAt), style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                Text(
                    "${Formatters.number(order.itemCount.toLong())} قلم کالا",
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                )
            }
            StatusChip(order.status)
        }

        Spacer(Modifier.height(Spacing.lg))
        OrderTimeline(order.status)

        Spacer(Modifier.height(Spacing.lg))
        Text("اقلام سفارش", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
            order.items.forEachIndexed { index, item ->
                if (index > 0) { Spacer(Modifier.height(Spacing.sm)); Divider(color = AtomTheme.colors.border); Spacer(Modifier.height(Spacing.sm)) }
                Row(
                    Modifier.fillMaxWidth().then(if (item.productId != null) Modifier.clickable { onProduct(item.productId) } else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(Radius.sm))) {
                        AtomImage(item.image, item.title, Modifier.fillMaxSize(), seed = item.id, placeholderIconSize = 20.dp)
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (item.seller.isNotBlank()) {
                            Text(item.seller, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                        }
                    }
                    PriceText(item.amount, emphasized = false)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Divider(color = AtomTheme.colors.border)
            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("مبلغ کل", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                PriceText(order.total)
            }
        }

        if (order.note.isNotBlank()) {
            Spacer(Modifier.height(Spacing.md))
            Text(order.note, style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
        }

        if (!order.address.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.lg))
            Text("اطلاعات ارسال", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.PersonPin, contentDescription = null, tint = AtomTheme.colors.textTertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        if (!order.recipient.isNullOrBlank()) Text(order.recipient, style = MaterialTheme.typography.bodyMedium)
                        if (!order.phone.isNullOrBlank()) {
                            Text(order.phone, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                        }
                        Text(order.address, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        SecondaryButton("پیام به پشتیبانی دربارهٔ این سفارش", onSupport, Modifier.fillMaxWidth())
    }
}

/** خط زمانی وضعیت سفارش (مانند صفحهٔ پیگیری سایت). */
@Composable
fun OrderTimeline(status: OrderStatus) {
    if (status == OrderStatus.RETURNED) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.dangerSoft).padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Undo, contentDescription = null, tint = AtomTheme.colors.danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "این سفارش مرجوع شده است. برای پیگیری بازگشت وجه با پشتیبانی در تماس باشید.",
                style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.danger,
            )
        }
        return
    }
    val last = OrderStatus.timeline.lastIndex
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OrderStatus.timeline.forEachIndexed { index, label ->
            val done = index <= status.step
            val current = index == status.step
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // خط اتصال مراحل: نیمهٔ سمت مرحلهٔ قبل و نیمهٔ سمت مرحلهٔ بعد،
                // تا مسیر پیشرفت در چیدمان راست‌به‌چپ پیوسته دیده شود
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (index > 0) {
                        Box(
                            Modifier.align(Alignment.CenterStart).fillMaxWidth(0.5f).height(2.dp)
                                .background(if (done) MaterialTheme.colorScheme.primary else AtomTheme.colors.border),
                        )
                    }
                    if (index < last) {
                        Box(
                            Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.5f).height(2.dp)
                                .background(if (index < status.step) MaterialTheme.colorScheme.primary else AtomTheme.colors.border),
                        )
                    }
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(if (done) MaterialTheme.colorScheme.primary else AtomTheme.colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done && !current) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else if (current) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(Formatters.number((index + 1).toLong()), style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
                    }
                }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done) MaterialTheme.colorScheme.onSurface else AtomTheme.colors.textTertiary,
                    textAlign = TextAlign.Center, maxLines = 2, minLines = 2,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/** پیگیری سفارش با کد و موبایل — برای مهمان و کاربر واردشده. */
@Composable
fun TrackScreen(
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: TrackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "پیگیری سفارش", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.md)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
        ) {
            Text(
                "کد سفارش و شمارهٔ موبایلی که هنگام ثبت سفارش وارد کرده‌اید را بنویسید.",
                style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.lg))
            AtomTextField(state.code, viewModel::setCode, "کد سفارش", kind = FieldKind.NUMBER, maxLength = 20)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(
                state.phone, viewModel::setPhone, "شمارهٔ موبایل", kind = FieldKind.PHONE,
                placeholder = "۰۹۱۲۳۴۵۶۷۸۹", maxLength = 15,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done, onImeAction = viewModel::track,
            )
            if (state.error != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger)
            }
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton("پیگیری", viewModel::track, Modifier.fillMaxWidth(), loading = state.loading)

            val order = state.order
            if (order != null) {
                Spacer(Modifier.height(Spacing.xl))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سفارش #${order.code}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusChip(order.status)
                }
                Spacer(Modifier.height(Spacing.md))
                OrderTimeline(order.status)
                Spacer(Modifier.height(Spacing.lg))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            PriceText(item.amount, emphasized = false)
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Divider(color = AtomTheme.colors.border)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("مبلغ کل", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        PriceText(order.total)
                    }
                }
                if (viewModel.isSignedIn && !state.claimed) {
                    Spacer(Modifier.height(Spacing.md))
                    SecondaryButton("افزودن این سفارش به حساب من", { viewModel.claim(onMessage) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
