package ir.atom313.app.ui.account

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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.ProductCardSkeleton
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.AppNotification
import ir.atom313.app.domain.model.NotificationType
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.LocalWindowWidth
import ir.atom313.app.ui.catalog.gridColumnsFor

/** ویرایش پروفایل — تغییرات با سایت همگام می‌شود. */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(state.saved) { if (state.saved) onMessage("اطلاعات شما ذخیره شد") }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "ویرایش پروفایل", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.screen),
        ) {
            AtomTextField(state.name, { v -> viewModel.update { it.copy(name = v) } }, "نام و نام خانوادگی", kind = FieldKind.NAME, error = state.errors["name"], maxLength = 120)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(state.phone, { v -> viewModel.update { it.copy(phone = v) } }, "شمارهٔ موبایل", kind = FieldKind.PHONE, error = state.errors["phone"], maxLength = 15)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(state.email, { v -> viewModel.update { it.copy(email = v) } }, "ایمیل", kind = FieldKind.EMAIL, error = state.errors["email"], maxLength = 160)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(state.city, { v -> viewModel.update { it.copy(city = v) } }, "شهر", kind = FieldKind.TEXT, error = state.errors["city"], maxLength = 60)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(
                state.address, { v -> viewModel.update { it.copy(address = v) } }, "آدرس پیش‌فرض ارسال",
                kind = FieldKind.ADDRESS, error = state.errors["address"], maxLength = 600,
                supportingText = "هنگام ثبت سفارش به‌صورت خودکار پر می‌شود.",
                imeAction = ImeAction.Done, onImeAction = viewModel::save,
            )
            if (state.generalError != null) {
                Spacer(Modifier.height(Spacing.md))
                Text(state.generalError!!, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger)
            }
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton("ذخیرهٔ تغییرات", viewModel::save, Modifier.fillMaxWidth(), loading = state.saving)
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

/** امنیت حساب — تغییر رمز و حذف حساب. */
@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(state.changed) { if (state.changed) onMessage("رمز عبور تغییر کرد. سایر دستگاه‌ها خارج شدند.") }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "امنیت حساب", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.screen)) {
            Text("تغییر رمز عبور", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(state.current, { v -> viewModel.update { it.copy(current = v) } }, "رمز فعلی", kind = FieldKind.PASSWORD, error = state.errors["current"], maxLength = 200)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(state.next, { v -> viewModel.update { it.copy(next = v) } }, "رمز جدید", kind = FieldKind.NEW_PASSWORD, error = state.errors["next"], supportingText = "حداقل ۸ نویسه", maxLength = 200)
            Spacer(Modifier.height(Spacing.md))
            AtomTextField(
                state.confirm, { v -> viewModel.update { it.copy(confirm = v) } }, "تکرار رمز جدید",
                kind = FieldKind.NEW_PASSWORD, error = state.errors["confirm"], maxLength = 200,
                imeAction = ImeAction.Done, onImeAction = viewModel::changePassword,
            )
            if (state.generalError != null) {
                Spacer(Modifier.height(Spacing.md))
                Text(state.generalError!!, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger)
            }
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton("تغییر رمز عبور", viewModel::changePassword, Modifier.fillMaxWidth(), loading = state.saving)

            Spacer(Modifier.height(Spacing.xxxl))
            Text("حذف حساب", style = MaterialTheme.typography.titleMedium, color = AtomTheme.colors.danger)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "با حذف حساب، اطلاعات شخصی شما پاک می‌شود و امکان ورود نخواهید داشت. " +
                    "سوابق مالی سفارش‌ها برای حسابداری فروشگاه نگهداری می‌شود اما به حساب شما متصل نخواهد بود. این کار بازگشت‌پذیر نیست.",
                style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton("حذف حساب کاربری", { confirmDelete = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Delete)
            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف حساب") },
            text = {
                Column {
                    Text("برای تأیید، رمز عبور خود را وارد کنید.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.md))
                    AtomTextField(
                        state.deletePassword, { v -> viewModel.update { it.copy(deletePassword = v) } },
                        "رمز عبور", kind = FieldKind.PASSWORD, error = state.deleteError, maxLength = 200,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount { confirmDelete = false; onAccountDeleted() } },
                    enabled = !state.deleting,
                ) { Text("حذف حساب", color = AtomTheme.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
            shape = RoundedCornerShape(Radius.lg),
        )
    }
}

/** تنظیمات — تم روشن/تاریک/سیستم. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appVersion: String,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "تنظیمات", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.screen)) {
            Text("پوستهٔ برنامه", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)) {
                ThemeOption("هماهنگ با سیستم", ThemeMode.SYSTEM, theme, viewModel::setTheme)
                ThemeOption("روشن", ThemeMode.LIGHT, theme, viewModel::setTheme)
                ThemeOption("تاریک", ThemeMode.DARK, theme, viewModel::setTheme)
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                "نسخهٔ برنامه: $appVersion",
                style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun ThemeOption(label: String, mode: ThemeMode, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = SemanticsRole.RadioButton) { onSelect(mode) }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** علاقه‌مندی‌ها — برای کاربر واردشده روی سرور (مشترک با سایت)، برای مهمان محلی. */
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onProduct: (Long) -> Unit,
    onExplore: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: WishlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ids by viewModel.ids.collectAsStateWithLifecycle()
    val columns = gridColumnsFor(LocalWindowWidth.current)

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "علاقه‌مندی‌ها", onBack = onBack)
        when {
            state.isInitialLoading -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                contentPadding = PaddingValues(Spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) { items(4) { ProductCardSkeleton() } }
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::load, modifier = Modifier.fillMaxSize())
            state.data.isNullOrEmpty() -> EmptyState(
                Icons.Outlined.FavoriteBorder, "فهرست علاقه‌مندی شما خالی است",
                "با زدن ♡ روی هر محصول، آن را اینجا ذخیره کنید.",
                "مشاهدهٔ محصولات", onExplore, Modifier.fillMaxSize(),
            )
            else -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                gridItems(state.data!!, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        wished = product.id in ids,
                        onClick = { onProduct(product.id) },
                        onToggleWish = { viewModel.toggle(product.id, onMessage) },
                        onAddToCart = { viewModel.addToCart(product, onMessage) },
                    )
                }
            }
        }
    }
}

/** اعلان‌ها — سفارش، پشتیبانی و دفاتر. لمس هر اعلان صفحهٔ مربوط را باز می‌کند. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOrder: (String) -> Unit,
    onSupport: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(
            title = "اعلان‌ها",
            onBack = onBack,
            actions = {
                if (state.data?.any { !it.read } == true) {
                    TextButton(onClick = viewModel::markAllRead) {
                        Text("خواندهٔ همه", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
        )
        when {
            state.isInitialLoading -> Column(Modifier.padding(Spacing.screen)) { repeat(5) { ListItemSkeleton() } }
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::load, modifier = Modifier.fillMaxSize())
            state.data.isNullOrEmpty() -> EmptyState(
                Icons.Outlined.NotificationsNone, "اعلانی ندارید",
                "وضعیت سفارش‌ها و پاسخ پشتیبانی اینجا نمایش داده می‌شود.",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.data!!, key = { it.id }) { notification ->
                    NotificationRow(notification) {
                        viewModel.markRead(notification.id)
                        when (notification.type) {
                            NotificationType.ORDER_PLACED, NotificationType.ORDER_STATUS ->
                                if (notification.ref.isNotBlank()) onOrder(notification.ref)
                            NotificationType.SUPPORT_REPLY, NotificationType.OFFICE_REPLY -> onSupport()
                            NotificationType.OTHER -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val (icon: ImageVector, tint) = when (notification.type) {
        NotificationType.ORDER_PLACED, NotificationType.ORDER_STATUS -> Icons.Outlined.LocalShipping to MaterialTheme.colorScheme.primary
        NotificationType.SUPPORT_REPLY, NotificationType.OFFICE_REPLY -> Icons.Outlined.SupportAgent to AtomTheme.colors.info
        NotificationType.OTHER -> Icons.Outlined.NotificationsNone to AtomTheme.colors.textSecondary
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md))
            .background(if (notification.read) MaterialTheme.colorScheme.surface else AtomTheme.colors.brandSoft)
            .clickable(onClick = onClick).padding(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (notification.body.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(notification.body, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(Formatters.relative(notification.createdAt), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
        }
        if (!notification.read) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}
