package ir.atom313.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * مسیرهای ناوبری. رشته‌ها در یک جا تعریف می‌شوند تا پیوند اعلان‌ها، پیوند سایت و
 * ناوبری داخلی همیشه هماهنگ بمانند.
 */
object Routes {
    // تب‌های اصلی (نوار پایین)
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MY_PRODUCTS = "my-products"
    const val CART = "cart"
    const val ACCOUNT = "account"

    // صفحه‌های داخلی
    const val PRODUCT = "product/{id}"
    const val SELLER = "seller/{id}"
    const val OFFICE = "office/{id}"
    const val SELLERS = "sellers"
    const val OFFICES = "offices"
    const val SEARCH = "search?q={q}"
    const val CHECKOUT = "checkout"
    const val ORDER_SUCCESS = "order-success/{code}"
    const val ORDERS = "orders"
    const val ORDER = "order/{code}"
    const val TRACK = "track?code={code}"
    const val WISHLIST = "wishlist"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SECURITY = "security"
    const val SUPPORT = "support"
    const val CHAT = "chat"
    const val FAQ = "faq"
    const val BLOG = "blog"
    const val ARTICLE = "article/{id}"
    const val ABOUT = "about"
    const val LOGIN = "login?next={next}"
    const val REGISTER = "register"

    fun product(id: Long) = "product/$id"
    fun seller(id: Long) = "seller/$id"
    fun office(id: Long) = "office/$id"
    fun article(id: Long) = "article/$id"
    fun order(code: String) = "order/${enc(code)}"
    fun orderSuccess(code: String) = "order-success/${enc(code)}"
    fun search(query: String = "") = "search?q=${enc(query)}"
    fun track(code: String = "") = "track?code=${enc(code)}"
    fun login(next: String? = null) = "login?next=${enc(next.orEmpty())}"

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")
    fun dec(v: String?): String = runCatching { URLDecoder.decode(v.orEmpty(), "UTF-8") }.getOrDefault(v.orEmpty())

    val tabs = listOf(HOME, EXPLORE, MY_PRODUCTS, CART, ACCOUNT)
}

/** ناوبری بدون ایجاد نمونهٔ تکراری هنگام لمس سریع (مثلاً دوبار زدن روی یک کارت). */
fun NavController.navigateSingleTop(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true; builder() }
}
