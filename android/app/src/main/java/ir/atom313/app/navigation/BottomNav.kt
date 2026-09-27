package ir.atom313.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * معماری اطلاعات اپ بر اساس ساختار سایت:
 *  خانه (اسلایدر، دسته‌ها، محصولات) · کاوش (کاتالوگ، فروشگاه‌ها، دفاتر محلات) ·
 *  محصولات من (خریدهای کاربر) · سبد · حساب.
 * «بلاگ»، «سؤالات متداول» و «پشتیبانی» از داخل حساب و صفحهٔ کاوش در دسترس‌اند تا نوار پایین شلوغ نشود.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "خانه", Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE(Routes.EXPLORE, "کاوش", Icons.Filled.GridView, Icons.Outlined.GridView),
    MY_PRODUCTS(Routes.MY_PRODUCTS, "محصولات من", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    CART(Routes.CART, "سبد", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    ACCOUNT(Routes.ACCOUNT, "حساب", Icons.Filled.Person, Icons.Outlined.Person),
}
