package ir.atom313.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.designsystem.theme.AtomTheme
import ir.atom313.app.core.designsystem.theme.DarkTokens
import ir.atom313.app.core.designsystem.theme.LightTokens
import ir.atom313.app.navigation.DeepLinks
import ir.atom313.app.ui.AtomAppRoot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // اسپلش فقط تا خوانده‌شدن نشست از حافظهٔ امن نگه داشته می‌شود (کسری از ثانیه)
        splash.setKeepOnScreenCondition { !viewModel.state.value.ready }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val startRoute = DeepLinks.toRoute(intent?.data)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            AtomTheme(state.theme) {
                AtomAppRoot(appState = state, appViewModel = viewModel, initialRoute = startRoute)
            }
        }

        // رنگ نوار وضعیت/ناوبری متناسب با تم انتخاب‌شده
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.map { it.theme }.collect { applySystemBars(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    private fun applySystemBars(mode: ThemeMode) {
        val dark = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        window.navigationBarColor = (if (dark) DarkTokens.Surface else LightTokens.Surface).toArgb()
    }
}
