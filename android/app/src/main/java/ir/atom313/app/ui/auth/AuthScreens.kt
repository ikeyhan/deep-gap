package ir.atom313.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.LogoMark
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.ui.component.AtomTopBar

/** ورود با حساب سایت (نام کاربری یا موبایل). */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    registrationOpen: Boolean,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) { if (state.success) onSuccess() }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "ورود به حساب", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.xl))
            LogoMark(Modifier.size(64.dp))
            Spacer(Modifier.height(Spacing.md))
            Text("خوش آمدید", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "با همان حساب سایت اتم ۳۱۳ وارد شوید.",
                style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xxl))

            Column(Modifier.widthIn(max = 460.dp)) {
                AtomTextField(
                    state.username, { v -> viewModel.update { it.copy(username = v) } },
                    "نام کاربری یا موبایل", kind = FieldKind.USERNAME,
                    leadingIcon = Icons.Outlined.Person, error = state.errors["username"], maxLength = 60,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.password, { v -> viewModel.update { it.copy(password = v) } },
                    "رمز عبور", kind = FieldKind.PASSWORD,
                    leadingIcon = Icons.Outlined.Lock, error = state.errors["password"], maxLength = 200,
                    imeAction = ImeAction.Done, onImeAction = viewModel::signIn,
                )
                if (state.generalError != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        state.generalError!!,
                        style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm))
                            .background(AtomTheme.colors.dangerSoft).padding(Spacing.md),
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("ورود", viewModel::signIn, Modifier.fillMaxWidth(), loading = state.loading)
                Spacer(Modifier.height(Spacing.md))
                if (registrationOpen) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("حساب ندارید؟", style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                        TextButton(onClick = onRegister) { Text("ثبت‌نام") }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "حساب‌های مدیریتی از طریق پنل مدیریت سایت وارد می‌شوند.",
                    style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** ثبت‌نام خریدار — همان قوانین سایت. */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) { if (state.success) onSuccess() }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "ساخت حساب", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 460.dp)) {
                Text("ساخت حساب خریدار", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "با این حساب می‌توانید در سایت و اپ خرید کنید و سفارش‌هایتان را ببینید.",
                    style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.xl))

                AtomTextField(
                    state.name, { v -> viewModel.update { it.copy(name = v) } }, "نام و نام خانوادگی",
                    kind = FieldKind.NAME, leadingIcon = Icons.Outlined.Badge, error = state.errors["name"], maxLength = 120,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.phone, { v -> viewModel.update { it.copy(phone = v) } }, "شمارهٔ موبایل",
                    kind = FieldKind.PHONE, leadingIcon = Icons.Outlined.PhoneAndroid, error = state.errors["phone"],
                    placeholder = "۰۹۱۲۳۴۵۶۷۸۹", maxLength = 15,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.username, { v -> viewModel.update { it.copy(username = v) } }, "نام کاربری",
                    kind = FieldKind.USERNAME, leadingIcon = Icons.Outlined.Person, error = state.errors["username"],
                    supportingText = "حروف و اعداد انگلیسی، حداقل ۳ نویسه", maxLength = 30,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.password, { v -> viewModel.update { it.copy(password = v) } }, "رمز عبور",
                    kind = FieldKind.NEW_PASSWORD, leadingIcon = Icons.Outlined.Lock, error = state.errors["password"],
                    supportingText = "حداقل ۸ نویسه", maxLength = 200,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.email, { v -> viewModel.update { it.copy(email = v) } }, "ایمیل (اختیاری)",
                    kind = FieldKind.EMAIL, leadingIcon = Icons.Outlined.AlternateEmail, error = state.errors["email"], maxLength = 160,
                )
                Spacer(Modifier.height(Spacing.md))
                AtomTextField(
                    state.city, { v -> viewModel.update { it.copy(city = v) } }, "شهر (اختیاری)",
                    kind = FieldKind.TEXT, leadingIcon = Icons.Outlined.LocationCity, maxLength = 60,
                    imeAction = ImeAction.Done, onImeAction = viewModel::signUp,
                )

                if (state.generalError != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        state.generalError!!,
                        style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.danger,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm))
                            .background(AtomTheme.colors.dangerSoft).padding(Spacing.md),
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("ساخت حساب", viewModel::signUp, Modifier.fillMaxWidth(), loading = state.loading)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}
