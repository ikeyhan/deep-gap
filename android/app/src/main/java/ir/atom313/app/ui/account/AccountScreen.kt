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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.LogoMark
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.Role
import ir.atom313.app.ui.component.AtomTopBar

/**
 * حساب — مرکز دسترسی کاربر: پروفایل، سفارش‌ها، علاقه‌مندی‌ها، اعلان‌ها،
 * پشتیبانی، تنظیمات و امنیت. برای مهمان، کارت ورود نمایش داده می‌شود.
 */
@Composable
fun AccountScreen(
    isSignedIn: Boolean,
    unreadCount: Int,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onOrders: () -> Unit,
    onTrack: () -> Unit,
    onWishlist: () -> Unit,
    onNotifications: () -> Unit,
    onSupport: () -> Unit,
    onFaq: () -> Unit,
    onBlog: () -> Unit,
    onSettings: () -> Unit,
    onSecurity: () -> Unit,
    onAbout: () -> Unit,
    onBusinessPanel: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(isSignedIn) { viewModel.refresh() }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "حساب کاربری")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
        ) {
            if (isSignedIn && user != null) {
                val u = user!!
                Row(
                    Modifier.padding(Spacing.screen).fillMaxWidth().clip(RoundedCornerShape(Radius.lg))
                        .background(MaterialTheme.colorScheme.surface).clickable(onClick = onProfile).padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                        contentAlignment = Alignment.Center,
                    ) { Text(u.initial, style = MaterialTheme.typography.headlineSmall, color = AtomTheme.colors.brandOnSoft) }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(u.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            u.phone.ifBlank { u.username },
                            style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textTertiary,
                        )
                        if (u.role != Role.CUSTOMER && u.storeName.isNotBlank()) {
                            Text(
                                if (u.role == Role.SELLER) "فروشگاه: ${u.storeName}" else "دفتر: ${u.storeName}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "ویرایش پروفایل", tint = AtomTheme.colors.textTertiary)
                }

                Row(
                    Modifier.padding(horizontal = Spacing.screen).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    StatCard("سفارش‌ها", counts.orders, Modifier.weight(1f), onOrders)
                    StatCard("علاقه‌مندی", counts.wishlist, Modifier.weight(1f), onWishlist)
                    StatCard("اعلان نخوانده", counts.unreadNotifications, Modifier.weight(1f), onNotifications)
                }
            } else {
                Column(
                    Modifier.padding(Spacing.screen).fillMaxWidth().clip(RoundedCornerShape(Radius.lg))
                        .background(MaterialTheme.colorScheme.surface).padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LogoMark(Modifier.size(48.dp))
                    Spacer(Modifier.height(Spacing.md))
                    Text("به اتم ۳۱۳ خوش آمدید", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "برای دیدن سفارش‌ها، محصولات و علاقه‌مندی‌ها وارد حساب خود شوید.",
                        style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton("ورود یا ثبت‌نام", onLogin, Modifier.fillMaxWidth())
                }
            }

            // فروشنده و دفتر محله حساب کارِ خود را در پنل وب دارند؛ اپ برای خرید است.
            // بدون این کارت، آن‌ها وارد می‌شوند و هیچ مسیری به پنل خودشان نمی‌بینند.
            val u = user
            if (isSignedIn && u != null && u.role != Role.CUSTOMER) {
                Row(
                    Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm).fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.infoSoft).padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (u.role == Role.SELLER) Icons.Outlined.Storefront else Icons.Outlined.LocationCity,
                        contentDescription = null, tint = AtomTheme.colors.info, modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (u.role == Role.SELLER) "پنل فروشندگان" else "پنل دفتر محله",
                            style = MaterialTheme.typography.titleSmall, color = AtomTheme.colors.info,
                        )
                        Text(
                            "مدیریت محصولات، سفارش‌ها و تنظیمات در وب‌سایت انجام می‌شود.",
                            style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.info,
                        )
                    }
                    TextButton(onClick = onBusinessPanel) { Text("باز کردن") }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            MenuSection("خرید و سفارش") {
                MenuRow(Icons.Outlined.ReceiptLong, "سفارش‌های من", onClick = if (isSignedIn) onOrders else onLogin)
                MenuRow(Icons.Outlined.LocalShipping, "پیگیری سفارش", onClick = onTrack)
                MenuRow(Icons.Outlined.FavoriteBorder, "علاقه‌مندی‌ها", onClick = onWishlist)
                MenuRow(
                    Icons.Outlined.Notifications, "اعلان‌ها",
                    badge = if (unreadCount > 0) Formatters.number(unreadCount.toLong()) else null,
                    onClick = if (isSignedIn) onNotifications else onLogin,
                )
            }

            MenuSection("پشتیبانی و راهنما") {
                MenuRow(Icons.Outlined.SupportAgent, "پشتیبانی و گفتگو", onClick = onSupport)
                MenuRow(Icons.AutoMirrored.Outlined.HelpOutline, "سؤالات متداول", onClick = onFaq)
                MenuRow(Icons.Outlined.Article, "بلاگ اتم", onClick = onBlog)
            }

            MenuSection("حساب و تنظیمات") {
                if (isSignedIn) {
                    MenuRow(Icons.Outlined.Person, "ویرایش پروفایل", onClick = onProfile)
                    MenuRow(Icons.Outlined.Lock, "امنیت حساب", onClick = onSecurity)
                }
                MenuRow(Icons.Outlined.Settings, "تنظیمات", onClick = onSettings)
                MenuRow(Icons.Outlined.Info, "دربارهٔ اتم ۳۱۳", onClick = onAbout)
                if (isSignedIn) {
                    MenuRow(
                        Icons.AutoMirrored.Outlined.Logout, "خروج از حساب",
                        tint = AtomTheme.colors.danger,
                        onClick = { confirmLogout = true },
                    )
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("خروج از حساب") },
            text = { Text("از حساب خود خارج می‌شوید؟ سبد خرید شما روی همین دستگاه باقی می‌ماند.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; viewModel.logout {} }) {
                    Text("خروج", color = AtomTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("انصراف") } },
            shape = RoundedCornerShape(Radius.lg),
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Formatters.number(value.toLong()), style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1)
    }
}

@Composable
private fun MenuSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
        Text(
            title, style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs),
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)) {
            content()
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: AtomTheme.colors.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(Spacing.md))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (badge != null) {
            Badge { Text(badge) }
            Spacer(Modifier.width(Spacing.sm))
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = AtomTheme.colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}
