package ir.atom313.app.screenshot

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import ir.atom313.app.R
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.Spacing
import ir.atom313.app.domain.model.Announcement
import ir.atom313.app.domain.model.Article
import ir.atom313.app.domain.model.Category
import ir.atom313.app.domain.model.Home
import ir.atom313.app.domain.model.Office
import ir.atom313.app.domain.model.Order
import ir.atom313.app.domain.model.OrderItem
import ir.atom313.app.domain.model.OrderStatus
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.ProductDetail
import ir.atom313.app.domain.model.Review
import ir.atom313.app.domain.model.Seller
import ir.atom313.app.domain.model.Slide
import ir.atom313.app.ui.home.HomeContent
import ir.atom313.app.ui.orders.OrderDetailContent
import ir.atom313.app.ui.product.ProductContent
import ir.atom313.app.ui.product.ReviewFormState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * رندر صفحه‌های کامل (نه فقط اجزا) با دادهٔ نمونه، تا چیدمان واقعی هر صفحه
 * در اندازهٔ گوشی و در هر دو تم قابل بررسی باشد.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "fa-rXX-w411dp-h891dp-xhdpi",
    application = android.app.Application::class,
)
class FullScreenRenderTest {

    @get:Rule val compose = createComposeRule()

    private fun product(id: Long, title: String, seller: String, price: Long, stock: Int = 6, rating: Double? = 4.6) =
        Product(
            id = id, title = title, category = "صنایع‌دستی", seller = seller, sellerId = id,
            price = price, stock = stock, inStock = stock > 0, image = null,
            rating = rating, reviewCount = if (rating == null) 0 else 18,
        )

    private val products = listOf(
        product(1, "کیف چرم طبیعی دست‌دوز مدل رویا", "آترا چرم", 2_450_000),
        product(2, "گلیم دستباف سنتی کرمان", "هنرکده پارسیان", 1_890_000),
        product(3, "سرویس چای‌خوری سرامیکی", "سفال سبز", 1_240_000),
        product(4, "میناکاری دستی روی مس", "هنر اصفهان", 980_000, stock = 0, rating = null),
    )

    private val home = Home(
        slides = listOf(
            Slide(1, "پیشنهاد ویژه", "صنایع‌دستی اصیل ایرانی", "ارسال رایگان برای خرید بالای ۵۰۰ هزار تومان", null, "مشاهدهٔ محصولات", ""),
            Slide(2, "تازه‌ها", "فروشگاه‌های تازه پیوسته", "", null, "", ""),
        ),
        announcement = Announcement("ارسال رایگان", "برای سفارش‌های بالای ۵۰۰ هزار تومان", "", ""),
        categories = listOf(Category("پوشاک", 31), Category("صنایع‌دستی", 24), Category("لوازم خانه", 18), Category("خواروبار", 12)),
        newest = products,
        popular = products.reversed(),
        sellers = listOf(
            Seller(1, "آترا چرم", "محصولات چرم طبیعی", "تهران", 4.9, 3200, null),
            Seller(2, "هنرکده پارسیان", "صنایع‌دستی اصیل", "اصفهان", 4.8, 1800, null),
        ),
        offices = listOf(Office(1, "دفتر محلهٔ ولنجک", "رضا کاظمی", "ولنجک", "تهران", null, 4.8)),
        articles = listOf(
            Article(1, "۷ نکته برای تشخیص چرم طبیعی از مصنوعی", "راهنمای خرید", "سارا محمدی", "با این هفت نشانه فریب نمی‌خورید.", null, 8940, "2026-01-20 10:00:00"),
        ),
    )

    private val detail = ProductDetail(
        product = products[0].copy(description = "کیف چرم طبیعی دست‌دوز با دوخت دستی و ضمانت اصالت کالا. مناسب استفادهٔ روزمره و اداری، با فضای داخلی جادار و بندی قابل تنظیم."),
        reviews = listOf(
            Review(1, "سارا محمدی", "کیفیت دوخت و چرم واقعاً عالی بود. بسته‌بندی هم مرتب رسید.", 5, "2026-02-10 12:00:00"),
            Review(2, "علی رضایی", "خوب بود ولی رنگش کمی تیره‌تر از عکس است.", 4, "2026-02-08 09:00:00"),
        ),
        ratingDistribution = listOf(0, 1, 2, 5, 10),
        related = products.drop(1),
    )

    private val order = Order(
        code = "1042", status = OrderStatus.SHIPPING, createdAt = "2026-02-01 09:00:00",
        total = 3_130_000, itemCount = 2,
        items = listOf(
            OrderItem(1, 1, "کیف چرم طبیعی دست‌دوز مدل رویا", 1, "آترا چرم", 2_450_000, OrderStatus.SHIPPING, null),
            OrderItem(2, 2, "گلیم دستباف سنتی کرمان", 1, "هنرکده پارسیان", 680_000, OrderStatus.SHIPPING, null),
        ),
        note = "ارسال رایگان", recipient = "مینا رضایی", phone = "09120001122",
        address = "تهران، خیابان آزادی، کوچهٔ بهار، پلاک ۱۲، واحد ۳",
    )

    private fun render(name: String, theme: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            AtomTheme(theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun homeLight() = render("screen-home-light", ThemeMode.LIGHT) { HomeBody() }
    @Test fun homeDark() = render("screen-home-dark", ThemeMode.DARK) { HomeBody() }

    @Composable private fun HomeBody() = HomeContent(
        home = home, wished = setOf(1L), contentPadding = PaddingValues(),
        onProduct = {}, onCategory = {}, onSeller = {}, onOffice = {}, onArticle = {},
        onAllProducts = {}, onAllSellers = {}, onAllOffices = {}, onAllArticles = {},
        onToggleWish = {}, onAddToCart = {},
    )

    @Test fun productLight() = render("screen-product-light", ThemeMode.LIGHT) { ProductBody() }
    @Test fun productDark() = render("screen-product-dark", ThemeMode.DARK) { ProductBody() }

    @Composable private fun ProductBody() = ProductContent(
        detail = detail, wished = true, reviewForm = ReviewFormState(), isSignedIn = true,
        onToggleWish = {}, onSeller = {}, onProduct = {},
        onReviewBody = {}, onReviewRating = {}, onSubmitReview = {}, onLogin = {},
        onAddRelatedToCart = {}, wishedIds = setOf(2L), onToggleWishFor = {}, bottomInset = 96.dp,
    )

    @Test fun orderDetailLight() = render("screen-order-light", ThemeMode.LIGHT) {
        OrderDetailContent(order, onProduct = {}, onSupport = {}, contentPadding = PaddingValues())
    }

    /** آیکون لانچر و نماد برند — بررسی اینکه لوگوی اتم درست رسم می‌شود. */
    @Test fun brandIcons() = render("brand-icons", ThemeMode.LIGHT) {
        Row(Modifier.padding(Spacing.screen), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Box(Modifier.size(96.dp)) {
                Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "آیکون اپ", modifier = Modifier.fillMaxSize())
            }
            Image(painterResource(R.drawable.ic_logo_mark), contentDescription = "نماد برند", modifier = Modifier.size(96.dp))
        }
    }
}
