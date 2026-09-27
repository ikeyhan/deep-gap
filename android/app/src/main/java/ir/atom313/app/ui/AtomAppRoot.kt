package ir.atom313.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.atom313.app.AppUiState
import ir.atom313.app.AppViewModel
import ir.atom313.app.core.designsystem.theme.Motion
import ir.atom313.app.navigation.Routes
import ir.atom313.app.navigation.TopLevelDestination
import ir.atom313.app.ui.component.OfflineBanner
import ir.atom313.app.ui.maintenance.MaintenanceScreen
import ir.atom313.app.ui.util.LocalWindowWidth
import kotlinx.coroutines.launch

/**
 * ریشهٔ رابط کاربری: نوار پایین، گراف ناوبری، پیام‌های سراسری (Snackbar)،
 * بنر آفلاین و صفحهٔ «در حال به‌روزرسانی».
 */
@Composable
fun AtomAppRoot(appState: AppUiState, appViewModel: AppViewModel, initialRoute: String?) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // پیوند عمیق ورودی (اعلان یا لینک سایت) پس از آماده‌شدن نشست
    LaunchedEffect(initialRoute, appState.ready) {
        if (appState.ready && initialRoute != null) navController.navigate(initialRoute)
    }

    // انقضای نشست: پیام + هدایت به ورود
    LaunchedEffect(Unit) {
        appViewModel.sessionExpired.collect {
            snackbarHostState.showSnackbar("نشست شما منقضی شد. دوباره وارد شوید.", duration = SnackbarDuration.Long)
            navController.navigate(Routes.login())
        }
    }

    if (appState.maintenance) {
        MaintenanceScreen(onRetry = appViewModel::onAppResumed)
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.tabs

    val snackbar: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    // عرض پنجره در اختیار همهٔ صفحه‌ها تا چیدمان با تبلت و تاشو هماهنگ شود
    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    CompositionLocalProvider(LocalWindowWidth provides windowWidth) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AtomBottomBar(
                visible = showBottomBar,
                currentDestination = backStackEntry?.destination,
                cartCount = appState.cartCount,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize()) {
            OfflineBanner(appState.online)
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.fillMaxSize(),
                    // گذار افقی طبیعی در RTL: صفحهٔ جدید از چپ می‌آید
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.NORMAL, easing = Motion.EaseOut)) + fadeIn(tween(Motion.FAST)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.NORMAL, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)) },
                    popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.NORMAL, easing = Motion.EaseOut)) + fadeIn(tween(Motion.FAST)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.NORMAL, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)) },
                ) {
                    atomNavGraph(
                        navController = navController,
                        appState = appState,
                        appViewModel = appViewModel,
                        snackbar = snackbar,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
    }
}

/** تب‌های اصلی — با انتخاب مقصد بر اساس سلسله‌مراتب تا حالت ذخیره‌شده حفظ شود. */
@Composable
private fun AtomBottomBar(
    visible: Boolean,
    currentDestination: androidx.navigation.NavDestination?,
    cartCount: Int,
    onSelect: (TopLevelDestination) -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically { it },
        exit = androidx.compose.animation.slideOutVertically { it },
    ) {
        AtomNavigationBar(
            destinations = TopLevelDestination.entries,
            cartCount = cartCount,
            isSelected = { dest -> currentDestination?.hierarchy?.any { it.route == dest.route } == true },
            onSelect = onSelect,
        )
    }
}
