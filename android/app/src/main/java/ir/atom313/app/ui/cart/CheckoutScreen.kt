package ir.atom313.app.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.shareText

/**
 * تسویه — فرم اطلاعات ارسال با صفحه‌کلید مناسب و اعتبارسنجی، صورت‌حساب سرور و ثبت سفارش.
 * درگاه پرداخت آنلاین هنوز به سایت متصل نیست؛ همان روند سایت (هماهنگی پس از ثبت) دنبال می‌شود.
 */
@Composable
fun CheckoutScreen(
    coupon: String?,
    paymentNote: String,
    onBack: () -> Unit,
    onPlaced: (String) -> Unit,
    onLogin: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(coupon) { viewModel.start(coupon) }
    LaunchedEffect(state.placedCode) { state.placedCode?.let(onPlaced) }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "تکمیل سفارش", onBack = onBack)
        when {
            state.loadingQuote && state.quote == null && state.generalError == null -> CenteredLoading()
            state.quote == null && state.generalError != null ->
                ErrorState(state.generalError!!, onRetry = viewModel::refreshQuote, onHome = onBack, modifier = Modifier.fillMaxSize())
            state.quote == null -> ErrorState(
                ir.atom313.app.core.common.AppError.Validation("cart_empty", "سبد خرید خالی است."),
                onRetry = null, onHome = onBack, modifier = Modifier.fillMaxSize(),
            )
            else -> {
                val quote = state.quote!!
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding()) {
                    if (state.isGuest) {
                        Row(
                            Modifier.padding(Spacing.screen).fillMaxWidth().clip(RoundedCornerShape(Radius.md))
                                .background(AtomTheme.colors.infoSoft).padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = AtomTheme.colors.info, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text("بدون ورود هم می‌توانید سفارش ثبت کنید", style = MaterialTheme.typography.labelLarge, color = AtomTheme.colors.info)
                                Text(
                                    "با ورود، سفارش در «محصولات من» ثبت و پیگیری آسان‌تر می‌شود.",
                                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.info,
                                )
                            }
                            androidx.compose.material3.TextButton(onClick = onLogin) { Text("ورود") }
                        }
                    }

                    Column(Modifier.padding(horizontal = Spacing.screen)) {
                        Text("اطلاعات گیرنده", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.form.name, { v -> viewModel.update { it.copy(name = v) } }, "نام و نام خانوادگی",
                            kind = FieldKind.NAME, error = state.errors["name"], maxLength = 120,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.form.phone, { v -> viewModel.update { it.copy(phone = v) } }, "شمارهٔ موبایل",
                            kind = FieldKind.PHONE, error = state.errors["phone"], placeholder = "۰۹۱۲۳۴۵۶۷۸۹", maxLength = 15,
                            supportingText = "برای هماهنگی ارسال و پیگیری سفارش",
                        )
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.form.city, { v -> viewModel.update { it.copy(city = v) } }, "شهر",
                            kind = FieldKind.TEXT, error = state.errors["city"], maxLength = 60,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.form.address, { v -> viewModel.update { it.copy(address = v) } }, "آدرس کامل پستی",
                            kind = FieldKind.ADDRESS, error = state.errors["address"], maxLength = 600,
                            placeholder = "خیابان، کوچه، پلاک، واحد و کد پستی",
                        )
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.form.note, { v -> viewModel.update { it.copy(note = v) } }, "یادداشت برای فروشنده (اختیاری)",
                            kind = FieldKind.MULTILINE, maxLength = 300, imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        )

                        Spacer(Modifier.height(Spacing.xl))
                        Text("صورت‌حساب", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                            quote.lines.forEach { line ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text(
                                        line.title + if (line.qty > 1) " × ${Formatters.number(line.qty.toLong())}" else "",
                                        style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary,
                                        modifier = Modifier.weight(1f), maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(Formatters.number(line.amount), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            androidx.compose.material3.Divider(color = AtomTheme.colors.border)
                            Spacer(Modifier.height(Spacing.sm))
                            BillRow("جمع کالاها", Formatters.toman(quote.subtotal))
                            if (quote.discount > 0) BillRow("تخفیف", "− " + Formatters.toman(quote.discount), AtomTheme.colors.danger)
                            BillRow("هزینهٔ ارسال", if (quote.shipping == 0L) "رایگان" else Formatters.toman(quote.shipping))
                        }

                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.surface2).padding(Spacing.md),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = AtomTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text(paymentNote, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
                        }

                        if (state.generalError != null && state.errors.isEmpty()) {
                            Spacer(Modifier.height(Spacing.md))
                            Text(
                                state.generalError!!.message,
                                style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger,
                            )
                        }
                        Spacer(Modifier.height(Spacing.xl))
                    }
                }

                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(Spacing.screen)
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("مبلغ نهایی", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                            PriceText(quote.total)
                        }
                        PrimaryButton("ثبت سفارش", viewModel::submit, loading = state.submitting)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, color: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color ?: MaterialTheme.colorScheme.onSurface)
    }
}

/** تأیید ثبت سفارش — کد سفارش، کپی و مسیر پیگیری. */
@Composable
fun OrderSuccessScreen(
    code: String,
    onHome: () -> Unit,
    onOrder: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(Spacing.xxl).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CheckCircle, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp),
        )
        Spacer(Modifier.height(Spacing.xl))
        Text("سفارش شما ثبت شد", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "برای هماهنگی پرداخت و ارسال به‌زودی با شما تماس گرفته می‌شود.",
            style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Row(
            Modifier.clip(RoundedCornerShape(Radius.md)).background(AtomTheme.colors.brandSoft).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("کد سفارش", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.brandOnSoft)
                Text(code, style = MaterialTheme.typography.headlineSmall, color = AtomTheme.colors.brandOnSoft)
            }
            Spacer(Modifier.width(Spacing.md))
            androidx.compose.material3.IconButton(onClick = {
                clipboard.setText(AnnotatedString(code))
                onMessage("کد سفارش کپی شد")
            }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "کپی کد سفارش", tint = AtomTheme.colors.brandOnSoft) }
        }
        Spacer(Modifier.height(Spacing.xl))
        PrimaryButton("پیگیری سفارش", { onOrder(code) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton("اشتراک‌گذاری کد", { shareText(context, "کد سفارش من در اتم ۳۱۳: $code") }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        androidx.compose.material3.TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به خانه") }
    }
}
