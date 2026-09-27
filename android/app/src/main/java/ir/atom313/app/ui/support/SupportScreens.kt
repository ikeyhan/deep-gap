package ir.atom313.app.ui.support

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.AppConfig
import ir.atom313.app.domain.model.OfficeThread
import ir.atom313.app.domain.model.Ticket
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.dial
import ir.atom313.app.ui.util.email
import ir.atom313.app.ui.util.openExternal

/**
 * پشتیبانی — کانال‌های واقعی موجود در سایت: پیام به تیم پشتیبانی (با نمایش پاسخ)،
 * گفتگوی هوشمند، سؤالات متداول، تلگرام، تماس و ایمیل.
 */
@Composable
fun SupportScreen(
    config: AppConfig,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onFaq: () -> Unit,
    onChat: () -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "پشتیبانی", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "channels") {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)) {
                    if (config.aiChatEnabled && config.chatEnabled) {
                        ChannelRow(Icons.Outlined.SmartToy, "گفتگوی هوشمند", "پاسخ فوری به سؤالات رایج", onChat)
                    }
                    ChannelRow(Icons.AutoMirrored.Outlined.HelpOutline, "سؤالات متداول", "پاسخ پرتکرارترین پرسش‌ها", onFaq)
                    if (config.telegram.isNotBlank()) {
                        ChannelRow(Icons.AutoMirrored.Outlined.Send, "تلگرام", config.telegram) { openExternal(context, config.telegram) }
                    }
                    if (config.phone.isNotBlank()) {
                        ChannelRow(Icons.Outlined.Phone, "تماس تلفنی", config.phone) { dial(context, config.phone) }
                    }
                    if (config.email.isNotBlank()) {
                        ChannelRow(Icons.Outlined.Email, "ایمیل", config.email) { email(context, config.email) }
                    }
                }
            }

            item(key = "form-header") {
                Spacer(Modifier.height(Spacing.md))
                Text("پیام به تیم پشتیبانی", style = MaterialTheme.typography.titleMedium)
            }

            if (!viewModel.isSignedIn) {
                item(key = "login") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                        Text(
                            "برای ارسال پیام و دیدن پاسخ پشتیبانی وارد حساب خود شوید.",
                            style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        PrimaryButton("ورود به حساب", onLogin)
                    }
                }
            } else {
                item(key = "form") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
                        AtomTextField(
                            state.subject, viewModel::setSubject, "موضوع (اختیاری)",
                            kind = FieldKind.TEXT, placeholder = "مثلاً پیگیری سفارش #۱۰۰۱", maxLength = 200,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        AtomTextField(
                            state.body, viewModel::setBody, "متن پیام",
                            kind = FieldKind.MULTILINE, error = state.errors["body"] ?: state.generalError, maxLength = 4000,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        PrimaryButton(
                            "ارسال پیام",
                            { viewModel.send { onMessage("پیام شما برای پشتیبانی ارسال شد") } },
                            loading = state.sending,
                        )
                    }
                }

                item(key = "tickets-header") {
                    Spacer(Modifier.height(Spacing.md))
                    Text("پیام‌های من", style = MaterialTheme.typography.titleMedium)
                }

                when {
                    state.tickets.isInitialLoading -> item { repeat(3) { ListItemSkeleton() } }
                    state.tickets.isFullError -> item { ErrorState(state.tickets.error!!, onRetry = viewModel::load, onLogin = onLogin) }
                    state.tickets.data.isNullOrEmpty() -> item {
                        EmptyState(Icons.Outlined.SupportAgent, "پیامی ارسال نکرده‌اید", message = "سؤال یا مشکل خود را از فرم بالا بفرستید.")
                    }
                    else -> items(state.tickets.data!!, key = { "t${it.id}" }) { TicketCard(it) }
                }

                if (state.officeThreads.isNotEmpty()) {
                    item(key = "office-header") {
                        Spacer(Modifier.height(Spacing.md))
                        Text("گفتگو با دفاتر محلات", style = MaterialTheme.typography.titleMedium)
                    }
                    items(state.officeThreads, key = { "o${it.id}" }) { OfficeThreadCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ticket.subject.ifBlank { "پیام پشتیبانی" },
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                ticket.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (ticket.reply.isNotBlank()) AtomTheme.colors.success else AtomTheme.colors.warning,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(ticket.body, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(Formatters.relative(ticket.createdAt), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
        if (ticket.reply.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.brandSoft).padding(Spacing.sm)) {
                Text("پاسخ پشتیبانی", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.brandOnSoft)
                Spacer(Modifier.height(2.dp))
                Text(ticket.reply, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.brandOnSoft)
            }
        }
    }
}

@Composable
private fun OfficeThreadCard(thread: OfficeThread) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface).padding(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(thread.office, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Formatters.relative(thread.createdAt), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(thread.body, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary)
        if (thread.replied) {
            Spacer(Modifier.height(Spacing.sm))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.brandSoft).padding(Spacing.sm)) {
                Text("پاسخ دفتر", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.brandOnSoft)
                Spacer(Modifier.height(2.dp))
                Text(thread.reply, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.brandOnSoft)
            }
        }
    }
}

/** گفتگوی هوشمند — رابط چت با حباب‌های پیام و ورودی چسبیده به پایین. */
@Composable
fun ChatScreen(
    config: AppConfig,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.start(config.chatWelcome.ifBlank { "سلام! من دستیار پشتیبانی اتم ۳۱۳ هستم. چطور می‌توانم کمکتان کنم؟" })
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "گفتگو با پشتیبانی", subtitle = "پاسخ‌های هوشمند و سریع", onBack = onBack)
        LazyColumn(
            Modifier.weight(1f).imePadding(),
            state = listState,
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(state.messages.size) { index ->
                val message = state.messages[index]
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
                ) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 300.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = Radius.md, topEnd = Radius.md,
                                    bottomStart = if (message.fromUser) Radius.md else 4.dp,
                                    bottomEnd = if (message.fromUser) 4.dp else Radius.md,
                                ),
                            )
                            .background(if (message.fromUser) MaterialTheme.colorScheme.primary else AtomTheme.colors.surface2)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }
            if (state.sending) {
                item {
                    Row(Modifier.padding(start = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("در حال نوشتن…", style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                    }
                }
            }
            if (state.error != null) {
                item {
                    Text(
                        state.error!!,
                        style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.danger,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.dangerSoft).padding(Spacing.sm),
                    )
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtomTextField(
                    state.input, viewModel::setInput, "پیام شما",
                    kind = FieldKind.TEXT, maxLength = 1000,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send, onImeAction = viewModel::send,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                IconButton(onClick = viewModel::send, enabled = state.input.isNotBlank() && !state.sending) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
