package ir.atom313.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import ir.atom313.app.AppUiState
import ir.atom313.app.AppViewModel
import ir.atom313.app.BuildConfig
import ir.atom313.app.navigation.DeepLinks
import ir.atom313.app.navigation.Routes
import ir.atom313.app.ui.account.AccountScreen
import ir.atom313.app.ui.account.NotificationsScreen
import ir.atom313.app.ui.account.ProfileScreen
import ir.atom313.app.ui.account.SecurityScreen
import ir.atom313.app.ui.account.SettingsScreen
import ir.atom313.app.ui.account.WishlistScreen
import ir.atom313.app.ui.auth.LoginScreen
import ir.atom313.app.ui.auth.RegisterScreen
import ir.atom313.app.ui.cart.CartScreen
import ir.atom313.app.ui.cart.CheckoutScreen
import ir.atom313.app.ui.cart.OrderSuccessScreen
import ir.atom313.app.ui.catalog.ExploreScreen
import ir.atom313.app.ui.catalog.OfficeScreen
import ir.atom313.app.ui.catalog.SearchScreen
import ir.atom313.app.ui.catalog.SellerScreen
import ir.atom313.app.ui.content.AboutScreen
import ir.atom313.app.ui.content.ArticleScreen
import ir.atom313.app.ui.content.BlogScreen
import ir.atom313.app.ui.content.FaqScreen
import ir.atom313.app.ui.home.HomeScreen
import ir.atom313.app.ui.orders.MyProductsScreen
import ir.atom313.app.ui.orders.OrderDetailScreen
import ir.atom313.app.ui.orders.OrdersScreen
import ir.atom313.app.ui.orders.TrackScreen
import ir.atom313.app.ui.product.ProductScreen
import ir.atom313.app.ui.support.ChatScreen
import ir.atom313.app.ui.support.SupportScreen

/**
 * گراف ناوبری اپ — تب‌های اصلی و صفحه‌های داخلی.
 * پیوندهای عمیق (اعلان‌ها و لینک‌های سایت) به همین مقصدها می‌رسند.
 */
fun NavGraphBuilder.atomNavGraph(
    navController: NavController,
    appState: AppUiState,
    appViewModel: AppViewModel,
    snackbar: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val isSignedIn = appState.session is ir.atom313.app.domain.model.SessionState.SignedIn
    fun back() = navController.popBackStack()
    fun goLogin() = navController.navigate(Routes.login())
    fun goProduct(id: Long) = navController.navigate(Routes.product(id))
    fun goOrder(code: String) = navController.navigate(Routes.order(code))
    val appVersion = BuildConfig.VERSION_NAME

    /* ---------- تب‌های اصلی ---------- */

    composable(Routes.HOME) {
        HomeScreen(
            unreadCount = appState.unreadNotifications,
            onProduct = ::goProduct,
            onSearch = { navController.navigate(Routes.search()) },
            onCategory = { navController.navigate(Routes.search(it)) },
            onSeller = { navController.navigate(Routes.seller(it)) },
            onOffice = { navController.navigate(Routes.office(it)) },
            onArticle = { navController.navigate(Routes.article(it)) },
            onAllProducts = { navController.navigate(Routes.EXPLORE) },
            onAllSellers = { navController.navigate(Routes.SELLERS) },
            onAllOffices = { navController.navigate(Routes.OFFICES) },
            onAllArticles = { navController.navigate(Routes.BLOG) },
            onNotifications = { if (isSignedIn) navController.navigate(Routes.NOTIFICATIONS) else goLogin() },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.EXPLORE) {
        ExploreScreen(
            onProduct = ::goProduct,
            onSeller = { navController.navigate(Routes.seller(it)) },
            onOffice = { navController.navigate(Routes.office(it)) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.MY_PRODUCTS) {
        MyProductsScreen(
            isSignedIn = isSignedIn,
            onLogin = ::goLogin,
            onTrack = { navController.navigate(Routes.track()) },
            onProduct = ::goProduct,
            onOrder = ::goOrder,
            onExplore = { navController.navigate(Routes.EXPLORE) },
            contentPadding = contentPadding,
        )
    }

    composable(Routes.CART) {
        CartScreen(
            onProduct = ::goProduct,
            onExplore = { navController.navigate(Routes.EXPLORE) },
            onCheckout = { coupon ->
                navController.currentBackStackEntry?.savedStateHandle?.set("coupon", coupon)
                navController.navigate(Routes.CHECKOUT)
            },
            contentPadding = contentPadding,
        )
    }

    composable(Routes.ACCOUNT) {
        AccountScreen(
            isSignedIn = isSignedIn,
            unreadCount = appState.unreadNotifications,
            onLogin = ::goLogin,
            onProfile = { if (isSignedIn) navController.navigate(Routes.PROFILE) else goLogin() },
            onOrders = { navController.navigate(Routes.ORDERS) },
            onTrack = { navController.navigate(Routes.track()) },
            onWishlist = { navController.navigate(Routes.WISHLIST) },
            onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
            onSupport = { navController.navigate(Routes.SUPPORT) },
            onFaq = { navController.navigate(Routes.FAQ) },
            onBlog = { navController.navigate(Routes.BLOG) },
            onSettings = { navController.navigate(Routes.SETTINGS) },
            onSecurity = { navController.navigate(Routes.SECURITY) },
            onAbout = { navController.navigate(Routes.ABOUT) },
            contentPadding = contentPadding,
        )
    }

    /* ---------- کاتالوگ ---------- */

    composable(
        route = Routes.PRODUCT,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinks.SCHEME}://product/{id}" }),
    ) {
        ProductScreen(
            onBack = { back() },
            onSeller = { navController.navigate(Routes.seller(it)) },
            onProduct = ::goProduct,
            onCart = { navController.navigate(Routes.CART) },
            onLogin = ::goLogin,
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.SEARCH, arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })) { entry ->
        SearchScreen(
            onBack = { back() },
            onProduct = ::goProduct,
            onMessage = snackbar,
            contentPadding = contentPadding,
            initialQuery = Routes.dec(entry.arguments?.getString("q")),
        )
    }

    composable(Routes.SELLERS) {
        ExploreScreen(
            onProduct = ::goProduct,
            onSeller = { navController.navigate(Routes.seller(it)) },
            onOffice = { navController.navigate(Routes.office(it)) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.OFFICES) {
        ExploreScreen(
            onProduct = ::goProduct,
            onSeller = { navController.navigate(Routes.seller(it)) },
            onOffice = { navController.navigate(Routes.office(it)) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.SELLER, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
        SellerScreen(onBack = { back() }, onProduct = ::goProduct, onMessage = snackbar, contentPadding = contentPadding)
    }

    composable(Routes.OFFICE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
        OfficeScreen(onBack = { back() }, contentPadding = contentPadding)
    }

    /* ---------- خرید ---------- */

    composable(Routes.CHECKOUT) {
        val coupon = navController.previousBackStackEntry?.savedStateHandle?.get<String>("coupon")
        CheckoutScreen(
            coupon = coupon,
            paymentNote = appState.config.paymentNote,
            onBack = { back() },
            onPlaced = { code ->
                navController.navigate(Routes.orderSuccess(code)) {
                    popUpTo(Routes.CART) { inclusive = false }
                }
            },
            onLogin = ::goLogin,
        )
    }

    composable(Routes.ORDER_SUCCESS, arguments = listOf(navArgument("code") { type = NavType.StringType })) { entry ->
        val code = Routes.dec(entry.arguments?.getString("code"))
        OrderSuccessScreen(
            code = code,
            onHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
            onOrder = { if (isSignedIn) goOrder(it) else navController.navigate(Routes.track(it)) },
            onMessage = snackbar,
        )
    }

    /* ---------- سفارش‌ها ---------- */

    composable(Routes.ORDERS, deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinks.SCHEME}://orders" })) {
        if (!isSignedIn) {
            LoginScreen(
                onBack = { back() },
                onSuccess = { back() },
                onRegister = { navController.navigate(Routes.REGISTER) },
                registrationOpen = appState.config.registrationOpen,
            )
        } else {
            OrdersScreen(
                onBack = { back() },
                onOrder = ::goOrder,
                onExplore = { navController.navigate(Routes.EXPLORE) },
                contentPadding = contentPadding,
            )
        }
    }

    composable(
        route = Routes.ORDER,
        arguments = listOf(navArgument("code") { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinks.SCHEME}://order/{code}" }),
    ) {
        OrderDetailScreen(
            onBack = { back() },
            onProduct = ::goProduct,
            onSupport = { navController.navigate(Routes.SUPPORT) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.TRACK, arguments = listOf(navArgument("code") { type = NavType.StringType; defaultValue = "" })) {
        TrackScreen(onBack = { back() }, onMessage = snackbar, contentPadding = contentPadding)
    }

    /* ---------- حساب ---------- */

    composable(Routes.WISHLIST) {
        WishlistScreen(
            onBack = { back() },
            onProduct = ::goProduct,
            onExplore = { navController.navigate(Routes.EXPLORE) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.NOTIFICATIONS, deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinks.SCHEME}://notifications" })) {
        NotificationsScreen(
            onBack = { back() },
            onOrder = ::goOrder,
            onSupport = { navController.navigate(Routes.SUPPORT) },
            contentPadding = contentPadding,
        )
    }

    composable(Routes.PROFILE) {
        ProfileScreen(onBack = { back() }, onMessage = snackbar)
    }

    composable(Routes.SECURITY) {
        SecurityScreen(
            onBack = { back() },
            onAccountDeleted = {
                snackbar("حساب شما حذف شد.")
                navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
            },
            onMessage = snackbar,
        )
    }

    composable(Routes.SETTINGS) {
        SettingsScreen(onBack = { back() }, appVersion = appVersion)
    }

    composable(Routes.LOGIN, arguments = listOf(navArgument("next") { type = NavType.StringType; defaultValue = "" })) {
        LoginScreen(
            onBack = { back() },
            onSuccess = { back() },
            onRegister = { navController.navigate(Routes.REGISTER) },
            registrationOpen = appState.config.registrationOpen,
        )
    }

    composable(Routes.REGISTER) {
        RegisterScreen(
            onBack = { back() },
            onSuccess = { navController.popBackStack(Routes.HOME, inclusive = false) },
        )
    }

    /* ---------- پشتیبانی و محتوا ---------- */

    composable(Routes.SUPPORT, deepLinks = listOf(navDeepLink { uriPattern = "${DeepLinks.SCHEME}://support" })) {
        SupportScreen(
            config = appState.config,
            onBack = { back() },
            onLogin = ::goLogin,
            onFaq = { navController.navigate(Routes.FAQ) },
            onChat = { navController.navigate(Routes.CHAT) },
            onMessage = snackbar,
            contentPadding = contentPadding,
        )
    }

    composable(Routes.CHAT) {
        ChatScreen(config = appState.config, onBack = { back() })
    }

    composable(Routes.FAQ) {
        FaqScreen(onBack = { back() }, onSupport = { navController.navigate(Routes.SUPPORT) }, contentPadding = contentPadding)
    }

    composable(Routes.BLOG) {
        BlogScreen(onBack = { back() }, onArticle = { navController.navigate(Routes.article(it)) }, contentPadding = contentPadding)
    }

    composable(Routes.ARTICLE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
        ArticleScreen(onBack = { back() }, contentPadding = contentPadding)
    }

    composable(Routes.ABOUT) {
        AboutScreen(config = appState.config, appVersion = appVersion, onBack = { back() }, contentPadding = contentPadding)
    }
}
