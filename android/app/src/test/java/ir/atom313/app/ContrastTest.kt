package ir.atom313.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import ir.atom313.app.core.designsystem.theme.BrandColors
import ir.atom313.app.core.designsystem.theme.DarkTokens
import ir.atom313.app.core.designsystem.theme.LightTokens
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * نگهبان دسترس‌پذیری رنگ‌ها (WCAG 2.1):
 *  - متن معمولی حداقل ۴٫۵:۱
 *  - متن بزرگ و عناصر گرافیکی معنادار حداقل ۳:۱
 * اگر کسی بعداً رنگی را عوض کند و کنتراست بشکند، این تست شکست می‌خورد.
 *
 * رنگ‌های «ملایم» تم تاریک آلفا دارند، پس ابتدا روی سطح ترکیب می‌شوند تا
 * همان پیکسلی سنجیده شود که کاربر می‌بیند.
 */
class ContrastTest {

    private fun ratio(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background).luminance().toDouble()
        val bg = background.luminance().toDouble()
        return (max(fg, bg) + 0.05) / (min(fg, bg) + 0.05)
    }

    private fun assertContrast(label: String, fg: Color, bg: Color, minimum: Double) {
        val value = ratio(fg, bg)
        assertTrue(
            "%s: %.2f:1 — below the required %.1f:1".format(label, value, minimum),
            value >= minimum,
        )
    }

    private val bodyText = 4.5
    private val largeTextOrIcon = 3.0

    /* ---------- تم روشن ---------- */

    @Test fun lightThemeText() {
        assertContrast("primary text on surface", LightTokens.Text1, LightTokens.Surface, bodyText)
        assertContrast("secondary text on surface", LightTokens.Text2, LightTokens.Surface, bodyText)
        assertContrast("primary text on page background", LightTokens.Text1, LightTokens.Bg, bodyText)
        assertContrast("secondary text on the muted surface", LightTokens.Text2, LightTokens.Surface2, bodyText)
        // متن کم‌رنگ (تاریخ‌ها، شمارنده‌ها) در اندازهٔ کوچک است اما هرگز تنها حامل معنا نیست
        assertContrast("tertiary text on surface", LightTokens.Text3, LightTokens.Surface, largeTextOrIcon)
    }

    @Test fun lightThemeBrand() {
        // برچسب دکمهٔ اصلی — پیش از این با سبز روشن‌تر فقط ۳٫۶:۱ بود
        assertContrast("button label on the primary button", Color.White, BrandColors.GradStart, bodyText)
        // متن تأکیدی سبز (نام دسته‌بندی، پیوندها) روی کارت سفید
        assertContrast("accent text on surface", BrandColors.GradStart, LightTokens.Surface, bodyText)
        assertContrast("brand text on the soft brand chip", BrandColors.Green700, BrandColors.Green050, bodyText)
        // ستارهٔ امتیاز معنا دارد، پس مثل یک عنصر گرافیکی سنجیده می‌شود
        assertContrast("rating star on surface", BrandColors.Gold700, LightTokens.Surface, largeTextOrIcon)
    }

    @Test fun lightThemeStatusChips() {
        assertContrast("delivered chip", BrandColors.Green600, BrandColors.Green050, largeTextOrIcon)
        assertContrast("returned chip", BrandColors.Red500, BrandColors.Red050, largeTextOrIcon)
        assertContrast("pending chip", Color(0xFFB26A00), Color(0xFFFFF4E0), largeTextOrIcon)
        assertContrast("shipping chip", Color(0xFF2563EB), Color(0xFFEAF1FE), largeTextOrIcon)
    }

    /* ---------- تم تاریک ---------- */

    @Test fun darkThemeText() {
        assertContrast("primary text on surface", DarkTokens.Text1, DarkTokens.Surface, bodyText)
        assertContrast("secondary text on surface", DarkTokens.Text2, DarkTokens.Surface, bodyText)
        assertContrast("primary text on page background", DarkTokens.Text1, DarkTokens.Bg, bodyText)
        assertContrast("tertiary text on surface", DarkTokens.Text3, DarkTokens.Surface, largeTextOrIcon)
    }

    @Test fun darkThemeBrand() {
        // در تم تاریک برچسب دکمه تیره است تا روی سبز روشن خوانده شود
        assertContrast("button label on the primary button", DarkTokens.Bg, BrandColors.Green500, bodyText)
        assertContrast("accent text on surface", BrandColors.Green500, DarkTokens.Surface, bodyText)
        assertContrast("rating star on surface", BrandColors.Gold500, DarkTokens.Surface, largeTextOrIcon)
    }

    @Test fun darkThemeStatusChips() {
        // پس‌زمینه‌های ملایم تم تاریک آلفا دارند و روی سطح ترکیب می‌شوند
        val surface = DarkTokens.Surface
        assertContrast("delivered chip", BrandColors.Green400, Color(0x1F22B14C).compositeOver(surface), largeTextOrIcon)
        assertContrast("returned chip", Color(0xFFFF6B6F), Color(0x24E5484D).compositeOver(surface), largeTextOrIcon)
        assertContrast("pending chip", Color(0xFFF6B73C), Color(0x1FF0A020).compositeOver(surface), largeTextOrIcon)
        assertContrast("shipping chip", Color(0xFF7AA7FF), Color(0x1F3B82F6).compositeOver(surface), largeTextOrIcon)
    }
}
