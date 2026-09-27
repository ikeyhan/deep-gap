package ir.atom313.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ir.atom313.app.R

/** Vazirmatn-FD (هم‌خانوادهٔ فونت سایت) — ارقام به‌صورت خودکار فارسی نمایش داده می‌شوند. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Vazirmatn, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = spacing.em,
)

// خط فارسی ارتفاع خط بیشتری نسبت به لاتین لازم دارد (مشابه line-height:1.75 سایت)
val AtomTypography = Typography(
    displaySmall = style(30, 44, FontWeight.ExtraBold),
    headlineLarge = style(26, 38, FontWeight.ExtraBold),
    headlineMedium = style(22, 34, FontWeight.ExtraBold),
    headlineSmall = style(20, 30, FontWeight.Bold),
    titleLarge = style(18, 28, FontWeight.Bold),
    titleMedium = style(16, 26, FontWeight.Bold),
    titleSmall = style(14, 22, FontWeight.Bold),
    bodyLarge = style(15, 26, FontWeight.Normal),
    bodyMedium = style(14, 24, FontWeight.Normal),
    bodySmall = style(12, 20, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Bold),
    labelMedium = style(12, 18, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)
