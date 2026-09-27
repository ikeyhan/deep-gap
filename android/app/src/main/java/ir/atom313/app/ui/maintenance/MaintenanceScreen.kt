package ir.atom313.app.ui.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.atom313.app.core.designsystem.component.EmptyState

/** سرور در حالت تعمیرات است (تنظیمات پنل مدیریت). */
@Composable
fun MaintenanceScreen(onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Outlined.Construction,
                title = "در حال به‌روزرسانی",
                message = "اتم ۳۱۳ چند لحظه‌ای در حال به‌روزرسانی است. لطفاً کمی بعد دوباره تلاش کنید.",
                actionLabel = "تلاش دوباره",
                onAction = onRetry,
            )
        }
    }
}
