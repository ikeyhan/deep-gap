package ir.atom313.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Radius
import ir.atom313.app.core.designsystem.theme.Spacing

/** حالت خالی حرفه‌ای: نماد، عنوان، توضیح و یک اقدام روشن. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(AtomTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                message, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton(actionLabel, onAction)
        }
    }
}

/** حالت خطای تمام‌صفحه با پیام قابل‌فهم و اقدام بازیابی مناسب همان خطا. */
@Composable
fun ErrorState(
    error: AppError,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onLogin: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
) {
    val (icon, title) = when (error) {
        AppError.Offline -> Icons.Outlined.WifiOff to "اینترنت در دسترس نیست"
        AppError.Timeout -> Icons.Outlined.HourglassEmpty to "پاسخ سرور طول کشید"
        is AppError.Maintenance -> Icons.Outlined.Construction to "در حال به‌روزرسانی"
        is AppError.Unauthorized -> Icons.Outlined.Lock to "نیاز به ورود دوباره"
        is AppError.NotFound -> Icons.Outlined.SearchOff to "یافت نشد"
        is AppError.Server -> Icons.Outlined.CloudOff to "مشکلی پیش آمد"
        else -> Icons.Outlined.ErrorOutline to "مشکلی پیش آمد"
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(AtomTheme.colors.surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = AtomTheme.colors.textSecondary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            error.message, style = MaterialTheme.typography.bodyMedium, color = AtomTheme.colors.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(Spacing.xl))
        when {
            error is AppError.Unauthorized && onLogin != null -> PrimaryButton("ورود دوباره", onLogin)
            onRetry != null && error !is AppError.NotFound -> PrimaryButton("تلاش دوباره", onRetry)
        }
        if (onHome != null && (error is AppError.NotFound || error is AppError.Forbidden)) {
            SecondaryButton("بازگشت به خانه", onHome)
        }
    }
}

/** بارگذاری تمام‌صفحه ساده (فقط وقتی اسکلتون مناسب نیست). */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

/**
 * نوار باریک «دادهٔ ذخیره‌شده»: وقتی به‌روزرسانی شکست خورده ولی محتوای قبلی نمایش داده می‌شود.
 */
@Composable
fun StaleBanner(error: AppError?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = error != null, enter = expandVertically(), exit = shrinkVertically(), modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                .clip(RoundedCornerShape(Radius.sm)).background(AtomTheme.colors.warningSoft)
                .padding(start = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = AtomTheme.colors.warning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                if (error == AppError.Offline) "نمایش آخرین اطلاعات ذخیره‌شده (آفلاین)" else "به‌روزرسانی انجام نشد؛ اطلاعات ذخیره‌شده نمایش داده می‌شود",
                style = MaterialTheme.typography.labelMedium, color = AtomTheme.colors.warning, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("تلاش دوباره", style = MaterialTheme.typography.labelMedium) }
        }
    }
}
