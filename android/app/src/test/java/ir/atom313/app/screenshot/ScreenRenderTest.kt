package ir.atom313.app.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.designsystem.component.AtomTextField
import ir.atom313.app.core.designsystem.component.EmptyState
import ir.atom313.app.core.designsystem.component.FieldKind
import ir.atom313.app.core.designsystem.component.PriceText
import ir.atom313.app.core.designsystem.component.PrimaryButton
import ir.atom313.app.core.designsystem.component.ProductCard
import ir.atom313.app.core.designsystem.component.ProductCardSkeleton
import ir.atom313.app.core.designsystem.component.SecondaryButton
import ir.atom313.app.core.designsystem.component.StatusChip
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.domain.model.Product
import ir.atom313.app.ui.orders.OrderTimeline
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * رندر واقعی اجزای رابط کاربری روی JVM (Robolectric + Roborazzi) و ذخیرهٔ تصویر.
 * هدف: بررسی چشمی چیدمان راست‌به‌چپ، فونت فارسی، رنگ برند و هر دو تم.
 *
 *   ./gradlew recordRoborazziDebug     → تولید تصاویر در app/build/outputs/roborazzi
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "fa-rXX-w411dp-h891dp-xhdpi",
    // اپ واقعی (Hilt + Keystore) لازم نیست و روی JVM بالا نمی‌آید؛ فقط رابط کاربری رندر می‌شود
    application = android.app.Application::class,
)
class ScreenRenderTest {

    @get:Rule val compose = createComposeRule()

    private val sample = Product(
        id = 1, title = "کیف چرم طبیعی دست‌دوز مدل رویا", category = "پوشاک", seller = "آترا چرم",
        sellerId = 1, price = 2_450_000, stock = 4, inStock = true, image = null,
        rating = 4.7, reviewCount = 12,
    )
    private val outOfStock = sample.copy(id = 2, title = "میناکاری دستی روی مس", seller = "هنر اصفهان", price = 980_000, stock = 0, inStock = false, rating = null, reviewCount = 0)

    private fun render(name: String, theme: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            AtomTheme(theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /* هر تصویر در تست جداگانه است، چون setContent در هر تست فقط یک‌بار قابل فراخوانی است. */

    @Test fun productCardsLight() = render("product-cards-light", ThemeMode.LIGHT) { ProductRow() }
    @Test fun productCardsDark() = render("product-cards-dark", ThemeMode.DARK) { ProductRow() }

    @Composable private fun ProductRow() {
        Row(Modifier.padding(Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProductCard(sample, wished = true, onClick = {}, onToggleWish = {}, onAddToCart = {}, modifier = Modifier.width(168.dp))
            ProductCard(outOfStock, wished = false, onClick = {}, onToggleWish = {}, onAddToCart = {}, modifier = Modifier.width(168.dp))
        }
    }

    @Test fun loadingSkeletons() = render("skeletons-light", ThemeMode.LIGHT) {
        Row(Modifier.padding(Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProductCardSkeleton(Modifier.width(168.dp))
            ProductCardSkeleton(Modifier.width(168.dp))
        }
    }

    @Test fun emptyCartLight() = render("empty-cart-light", ThemeMode.LIGHT) {
        EmptyState(
            icon = Icons.Outlined.ShoppingBag,
            title = "سبد خرید شما خالی است",
            message = "محصولات موردعلاقه‌تان را به سبد اضافه کنید تا اینجا ببینید.",
            actionLabel = "مشاهدهٔ محصولات",
            onAction = {},
        )
    }

    @Test fun emptyProductsDark() = render("empty-products-dark", ThemeMode.DARK) {
        EmptyState(
            icon = Icons.Outlined.Inventory2,
            title = "هنوز محصولی نخریده‌اید",
            message = "پس از اولین خرید، محصولات شما با وضعیت سفارش اینجا نمایش داده می‌شود.",
            actionLabel = "مشاهدهٔ محصولات",
            onAction = {},
        )
    }

    @Test fun orderStatusLight() = render("order-status-light", ThemeMode.LIGHT) { OrderBlock() }
    @Test fun orderStatusDark() = render("order-status-dark", ThemeMode.DARK) { OrderBlock() }

    @Composable private fun OrderBlock() {
        Column(Modifier.padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OrderStatus.entries.take(3).forEach { StatusChip(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusChip(OrderStatus.DELIVERED)
                StatusChip(OrderStatus.RETURNED)
            }
            Spacer(Modifier.height(Spacing.md))
            OrderTimeline(OrderStatus.SHIPPING)
            Spacer(Modifier.height(Spacing.md))
            PriceText(2_450_000)
        }
    }

    @Test fun formsLight() = render("forms-light", ThemeMode.LIGHT) { FormBlock() }
    @Test fun formsDark() = render("forms-dark", ThemeMode.DARK) { FormBlock() }

    @Composable private fun FormBlock() {
        Column(Modifier.padding(Spacing.screen).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text("ورود به حساب", style = MaterialTheme.typography.headlineSmall)
            AtomTextField("mina", {}, "نام کاربری یا موبایل", kind = FieldKind.USERNAME)
            AtomTextField("", {}, "شمارهٔ موبایل", kind = FieldKind.PHONE, error = "شمارهٔ موبایل نامعتبر است (نمونه: ۰۹۱۲۳۴۵۶۷۸۹).", placeholder = "۰۹۱۲۳۴۵۶۷۸۹")
            AtomTextField("secret123", {}, "رمز عبور", kind = FieldKind.PASSWORD, supportingText = "حداقل ۸ نویسه")
            PrimaryButton("ورود", {}, Modifier.fillMaxWidth())
            SecondaryButton("ساخت حساب", {}, Modifier.fillMaxWidth())
            PrimaryButton("در حال ارسال", {}, Modifier.fillMaxWidth(), loading = true)
        }
    }
}
