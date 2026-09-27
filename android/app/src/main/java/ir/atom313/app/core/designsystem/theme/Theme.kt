package ir.atom313.app.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.datastore.ThemeMode

/** رنگ‌های تکمیلی برند که در ColorScheme متریال جایی ندارند. */
@Immutable
data class AtomExtendedColors(
    val bg: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val brandSoft: Color,
    val brandOnSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val star: Color,
    val isDark: Boolean,
) {
    val brandGradient: Brush get() = Brush.linearGradient(listOf(BrandColors.GradStart, BrandColors.GradEnd))
}

private val LightExtended = AtomExtendedColors(
    bg = LightTokens.Bg, surface2 = LightTokens.Surface2, border = LightTokens.Border, borderStrong = LightTokens.BorderStrong,
    textSecondary = LightTokens.Text2, textTertiary = LightTokens.Text3,
    brandSoft = BrandColors.Green050, brandOnSoft = BrandColors.Green700,
    success = BrandColors.Green600, successSoft = BrandColors.Green050,
    warning = Color(0xFFB26A00), warningSoft = Color(0xFFFFF4E0),
    danger = BrandColors.Red500, dangerSoft = BrandColors.Red050,
    info = Color(0xFF2563EB), infoSoft = Color(0xFFEAF1FE),
    star = BrandColors.Gold700, isDark = false,
)

private val DarkExtended = AtomExtendedColors(
    bg = DarkTokens.Bg, surface2 = DarkTokens.Surface2, border = DarkTokens.Border, borderStrong = DarkTokens.BorderStrong,
    textSecondary = DarkTokens.Text2, textTertiary = DarkTokens.Text3,
    brandSoft = Color(0x1F22B14C), brandOnSoft = BrandColors.Green400,
    success = BrandColors.Green400, successSoft = Color(0x1F22B14C),
    warning = Color(0xFFF6B73C), warningSoft = Color(0x1FF0A020),
    danger = Color(0xFFFF6B6F), dangerSoft = Color(0x24E5484D),
    info = Color(0xFF7AA7FF), infoSoft = Color(0x1F3B82F6),
    star = BrandColors.Gold500, isDark = true,
)

private val LightScheme = lightColorScheme(
    // سبز تیره‌تر برند (ابتدای گرادیان سایت): متن سفید روی آن ۵٫۴:۱ کنتراست دارد،
    // در حالی که سبز اصلی ۳٫۶:۱ بود و برای متن، حد استاندارد WCAG AA را رد نمی‌کرد
    primary = BrandColors.GradStart, onPrimary = Color.White,
    primaryContainer = BrandColors.Green100, onPrimaryContainer = BrandColors.Green700,
    secondary = BrandColors.Green700, onSecondary = Color.White,
    secondaryContainer = BrandColors.Green050, onSecondaryContainer = BrandColors.Green700,
    tertiary = BrandColors.Gold500, onTertiary = Color.White,
    background = LightTokens.Bg, onBackground = LightTokens.Text1,
    surface = LightTokens.Surface, onSurface = LightTokens.Text1,
    surfaceVariant = LightTokens.Surface2, onSurfaceVariant = LightTokens.Text2,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = LightTokens.Surface, surfaceContainerHigh = LightTokens.Surface, surfaceContainerHighest = LightTokens.Surface2,
    outline = LightTokens.BorderStrong, outlineVariant = LightTokens.Border,
    error = BrandColors.Red500, onError = Color.White, errorContainer = BrandColors.Red050, onErrorContainer = Color(0xFF8C1D21),
    scrim = Color(0x99000000),
)

private val DarkScheme = darkColorScheme(
    // در تم تاریک، متن تیره روی سبز روشن خوانده می‌شود (۶٫۷:۱)؛ متن سفید فقط ۲٫۸:۱ بود
    primary = BrandColors.Green500, onPrimary = DarkTokens.Bg,
    primaryContainer = Color(0xFF123D22), onPrimaryContainer = BrandColors.Green100,
    secondary = BrandColors.Green400, onSecondary = DarkTokens.Bg,
    secondaryContainer = Color(0xFF16311F), onSecondaryContainer = BrandColors.Green100,
    tertiary = BrandColors.Gold500, onTertiary = DarkTokens.Bg,
    background = DarkTokens.Bg, onBackground = DarkTokens.Text1,
    surface = DarkTokens.Surface, onSurface = DarkTokens.Text1,
    surfaceVariant = DarkTokens.Surface2, onSurfaceVariant = DarkTokens.Text2,
    surfaceContainerLowest = DarkTokens.Bg, surfaceContainerLow = DarkTokens.Surface,
    surfaceContainer = DarkTokens.Surface, surfaceContainerHigh = DarkTokens.Surface2, surfaceContainerHighest = DarkTokens.Surface2,
    outline = DarkTokens.BorderStrong, outlineVariant = DarkTokens.Border,
    error = Color(0xFFFF6B6F), onError = DarkTokens.Bg, errorContainer = Color(0xFF3A1416), onErrorContainer = Color(0xFFFFDAD7),
    scrim = Color(0xCC000000),
)

/** شعاع گوشه‌ها — همان --radius-* سایت. */
object Radius {
    val xs = 8.dp; val sm = 10.dp; val md = 14.dp; val lg = 20.dp; val xl = 26.dp; val pill = 999.dp
}

/** فاصله‌گذاری ۴dp-محور */
object Spacing {
    val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
    /** فاصلهٔ افقی استاندارد صفحه */
    val screen = 16.dp
}

/** مدت و منحنی انیمیشن‌ها — همان --dur/--ease سایت (۱۶۰ تا ۲۸۰ میلی‌ثانیه). */
object Motion {
    const val FAST = 160
    const val NORMAL = 240
    const val SLOW = 320
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

private val AtomShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

val LocalAtomColors = staticCompositionLocalOf { LightExtended }

/** دسترسی کوتاه: AtomTheme.colors.border */
object AtomTheme {
    val colors: AtomExtendedColors @Composable get() = LocalAtomColors.current
}

@Composable
fun AtomTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalAtomColors provides if (dark) DarkExtended else LightExtended,
        // محتوای اپ فارسی است؛ چیدمان همیشه راست‌به‌چپ است، حتی اگر زبان دستگاه انگلیسی باشد
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AtomTypography,
            shapes = AtomShapes,
            content = content,
        )
    }
}
