package ir.atom313.app.ui.content

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import ir.atom313.app.core.designsystem.component.CenteredLoading
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.ErrorState
import ir.atom313.app.core.designsystem.component.ListItemSkeleton
import ir.atom313.app.core.designsystem.component.LogoMark
import ir.atom313.app.core.designsystem.component.SkeletonBox
import ir.atom313.app.core.designsystem.component.StaleBanner
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.data.paging.toAppError
import ir.atom313.app.domain.model.AppConfig
import ir.atom313.app.ui.component.AtomTopBar
import ir.atom313.app.ui.util.dial
import ir.atom313.app.ui.util.email
import ir.atom313.app.ui.util.openExternal

/** بلاگ — فهرست مطالب منتشرشدهٔ سایت. */
@Composable
fun BlogScreen(
    onBack: () -> Unit,
    onArticle: (Long) -> Unit,
    contentPadding: PaddingValues,
    viewModel: BlogViewModel = hiltViewModel(),
) {
    val items = viewModel.articles.collectAsLazyPagingItems()
    val refresh = items.loadState.refresh

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "بلاگ اتم", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> items(4) {
                    Column {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f), Radius.md)
                        Spacer(Modifier.height(Spacing.sm))
                        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(16.dp))
                    }
                }
                refresh is LoadState.Error && items.itemCount == 0 -> item { ErrorState(refresh.error.toAppError(), onRetry = items::retry) }
                items.itemCount == 0 && refresh is LoadState.NotLoading -> item {
                    EmptyState(Icons.Outlined.Article, "هنوز مطلبی منتشر نشده", "به‌زودی مطالب تازه اینجا قرار می‌گیرد.")
                }
            }
            items(items.itemCount, key = items.itemKey { it.id }) { index ->
                val article = items[index]
                if (article != null) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md))
                            .background(MaterialTheme.colorScheme.surface).clickable { onArticle(article.id) },
                    ) {
                        AtomImage(article.cover, article.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f), seed = article.id)
                        Column(Modifier.padding(Spacing.md)) {
                            if (article.category.isNotBlank()) {
                                Text(article.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (article.excerpt.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(article.excerpt, style = MaterialTheme.typography.bodySmall, color = AtomTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(article.author, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary, modifier = Modifier.weight(1f))
                                Text(Formatters.persianDate(article.createdAt), style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
                            }
                        }
                    }
                } else {
                    ListItemSkeleton()
                }
            }
            if (items.loadState.append is LoadState.Loading) {
                item { Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.5.dp) } }
            }
        }
    }
}

/** مقاله — متن HTML پنل مدیریت به‌صورت بومی نمایش داده می‌شود (بدون WebView). */
@Composable
fun ArticleScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: ArticleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val article = state.data

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = article?.title ?: "مطلب", onBack = onBack)
        StaleBanner(state.error.takeIf { article != null }, viewModel::retry)
        when {
            state.isInitialLoading -> CenteredLoading()
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::retry, onHome = onBack, modifier = Modifier.fillMaxSize())
            article != null -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xxl),
            ) {
                AtomImage(article.cover, article.title, Modifier.fillMaxWidth().aspectRatio(16f / 9f), seed = article.id)
                Column(Modifier.padding(Spacing.screen)) {
                    if (article.category.isNotBlank()) {
                        Text(article.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(article.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(article.author, style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("·", color = AtomTheme.colors.textTertiary)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(Formatters.persianDate(article.createdAt), style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    HtmlText(article.bodyHtml)
                }
            }
        }
    }
}

/** سؤالات متداول — فهرست بازشونده. */
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onSupport: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: FaqViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "سؤالات متداول", onBack = onBack)
        StaleBanner(state.error.takeIf { state.data != null }, viewModel::retry)
        when {
            state.isInitialLoading -> Column(Modifier.padding(Spacing.screen)) { repeat(5) { ListItemSkeleton(avatar = false) } }
            state.isFullError -> ErrorState(state.error!!, onRetry = viewModel::retry, modifier = Modifier.fillMaxSize())
            state.data.isNullOrEmpty() -> EmptyState(
                Icons.AutoMirrored.Outlined.HelpOutline, "سؤالی ثبت نشده",
                "برای پرسش خود می‌توانید به پشتیبانی پیام بدهید.",
                "پشتیبانی", onSupport, Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screen, end = Spacing.screen, top = Spacing.md,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.data!!, key = { it.id }) { faq ->
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)
                            .clickable { expanded = !expanded }.padding(Spacing.md).animateContentSize(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(faq.question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (expanded) "بستن" else "باز کردن",
                                tint = AtomTheme.colors.textTertiary,
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(faq.answer, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "پاسخ سؤالتان را پیدا نکردید؟",
                        style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    ir.atom313.app.core.designsystem.component.SecondaryButton("پیام به پشتیبانی", onSupport, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** دربارهٔ اتم ۳۱۳ — اطلاعات تماس از تنظیمات سایت. */
@Composable
fun AboutScreen(
    config: AppConfig,
    appVersion: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AtomTopBar(title = "دربارهٔ ما", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.screen)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) { LogoMark(Modifier.size(52.dp)) }
            Spacer(Modifier.height(Spacing.md))
            Text(config.siteName, style = MaterialTheme.typography.headlineSmall)
            if (config.siteDescription.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    config.siteDescription,
                    style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(MaterialTheme.colorScheme.surface)) {
                if (config.phone.isNotBlank()) ContactRow(Icons.Outlined.Phone, "تماس", config.phone) { dial(context, config.phone) }
                if (config.email.isNotBlank()) ContactRow(Icons.Outlined.Email, "ایمیل", config.email) { email(context, config.email) }
                if (config.address.isNotBlank()) ContactRow(Icons.Outlined.LocationOn, "نشانی", config.address, null)
                if (config.domain.isNotBlank()) ContactRow(Icons.Outlined.Language, "وب‌سایت", config.domain) { openExternal(context, config.domain) }
                if (config.telegram.isNotBlank()) ContactRow(Icons.AutoMirrored.Outlined.Send, "تلگرام", config.telegram) { openExternal(context, config.telegram) }
                if (config.instagram.isNotBlank()) ContactRow(Icons.Outlined.PhotoCamera, "اینستاگرام", config.instagram) { openExternal(context, config.instagram) }
            }
            Spacer(Modifier.height(Spacing.xl))
            Text("نسخهٔ $appVersion", style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.textTertiary)
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AtomTheme.colors.textTertiary)
            Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
