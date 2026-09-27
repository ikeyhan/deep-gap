package ir.atom313.app.ui.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * عرض فعلی پنجره — برای چیدمان واکنش‌گرا (گوشی کوچک، گوشی بزرگ، تاشو، تبلت).
 * در MainActivity/AtomAppRoot مقداردهی می‌شود تا هیچ صفحه‌ای اندازهٔ ثابت فرض نکند.
 */
val LocalWindowWidth = compositionLocalOf<Dp> { 400.dp }

/** آیا فضای افقی برای چیدمان دو ستونی هست؟ (تبلت/تاشوی باز) */
fun isExpanded(width: Dp): Boolean = width >= 720.dp
