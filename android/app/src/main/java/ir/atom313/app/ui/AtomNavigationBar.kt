package ir.atom313.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import ir.atom313.app.core.common.Formatters
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.navigation.Routes
import ir.atom313.app.navigation.TopLevelDestination

/** نوار ناوبری پایین با نشان تعداد سبد و انیمیشن ملایم آیکون انتخاب‌شده. */
@Composable
fun AtomNavigationBar(
    destinations: List<TopLevelDestination>,
    cartCount: Int,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { dest ->
            val selected = isSelected(dest)
            val scale by animateFloatAsState(if (selected) 1.08f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "navIcon")
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (dest.route == Routes.CART && cartCount > 0) {
                                Badge { Text(Formatters.number(cartCount.toLong())) }
                            }
                        },
                    ) {
                        Icon(
                            if (selected) dest.selectedIcon else dest.icon,
                            contentDescription = dest.label,
                            modifier = Modifier.size(24.dp).scale(scale),
                        )
                    }
                },
                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = AtomTheme.colors.brandSoft,
                    unselectedIconColor = AtomTheme.colors.textTertiary,
                    unselectedTextColor = AtomTheme.colors.textTertiary,
                ),
            )
        }
    }
}
