package ir.atom313.app.ui.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.CenteredLoading
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.component.StaleBanner
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.dial

/** صفحهٔ دفتر محله — خدمات دفتر و فرم گفتگو (پاسخ در بخش پشتیبانی حساب می‌آید). */
@Composable
fun OfficeScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: OfficeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val detail = state.data

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = detail?.office?.name ?: "دفتر محله", onBack = onBack)
        StaleBanner(state.error.takeIf { detail != null }, viewModel::retry)
        when {
            state.isInitialLoading -> CenteredLoading()
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::retry, onHome = onBack, modifier = Modifier.fillMaxSize())
            detail != null -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(key = "head") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(detail.office.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOf(detail.office.area, detail.office.city).filter { it.isNotBlank() }.joinToString("، "),
                                    style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary,
                                )
                            }
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "تأییدشده", tint = AtomTheme.colors.success, modifier = Modifier.size(20.dp))
                        }
                        if (detail.office.manager.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text("مسئول دفتر: ${detail.office.manager}", style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                        }
                        if (detail.office.address.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AtomTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(detail.office.address, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                            }
                        }
                        if (detail.office.bio.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(detail.office.bio, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                        }
                        if (detail.office.phone.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.md))
                            SecondaryButton("تماس با دفتر", { dial(context, detail.office.phone) }, icon = Icons.Outlined.Phone)
                        }
                    }
                }

                if (detail.services.isNotEmpty()) {
                    item(key = "services-h") { Text("خدمات این دفتر", style = MaterialTheme.typography.titleMedium) }
                    items(detail.services, key = { it.id }) { service ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(service.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                if (service.price > 0) PriceText(service.price, emphasized = false)
                            }
                            if (service.category.isNotBlank()) {
                                Text(service.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (service.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(service.description, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                            }
                        }
                    }
                }

                item(key = "form") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                        Text("گفتگو با دفتر", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        if (form.sent) {
                            Text(
                                "پیام شما برای این دفتر ارسال شد. پاسخ در بخش «پشتیبانی» حساب شما نمایش داده می‌شود.",
                                style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.success,
                            )
                        } else {
                            if (!viewModel.isSignedIn) {
                                AtomTextField(form.name, viewModel::setName, "نام شما", kind = FieldKind.NAME, error = form.errors["name"], maxLength = 120)
                                Spacer(Modifier.height(Spacing.sm))
                                AtomTextField(form.phone, viewModel::setPhone, "شمارهٔ موبایل", kind = FieldKind.PHONE, error = form.errors["phone"], placeholder = "۰۹۱۲۳۴۵۶۷۸۹", maxLength = 15)
                                Spacer(Modifier.height(Spacing.sm))
                            }
                            AtomTextField(
                                form.body, viewModel::setBody, "متن پیام",
                                kind = FieldKind.MULTILINE, error = form.errors["body"] ?: form.generalError,
                                placeholder = "سؤال یا درخواست خود را بنویسید…", maxLength = 4000,
                            )
                            Spacer(Modifier.height(Spacing.md))
                            PrimaryButton("ارسال پیام", viewModel::send, loading = form.sending)
                        }
                    }
                }
            }
        }
    }
}
