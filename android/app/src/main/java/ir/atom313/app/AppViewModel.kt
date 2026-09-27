package ir.atom313.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.core.network.NetworkMonitor
import ir.atom313.app.core.network.SessionEvents
import ir.atom313.app.data.repository.CartRepository
import ir.atom313.app.data.repository.ContentRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.AppConfig
import ir.atom313.app.domain.model.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** وضعیت سراسری پوستهٔ اپ (تم، نشست، اتصال، شمارندهٔ سبد و اعلان). */
data class AppUiState(
    val ready: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val session: SessionState = SessionState.Unknown,
    val online: Boolean = true,
    val cartCount: Int = 0,
    val unreadNotifications: Int = 0,
    val config: AppConfig = AppConfig(),
    val maintenance: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val session: SessionRepository,
    private val wishlist: WishlistRepository,
    cart: CartRepository,
    content: ContentRepository,
    networkMonitor: NetworkMonitor,
    private val events: SessionEvents,
) : ViewModel() {

    val state: StateFlow<AppUiState> = combine(
        prefs.theme,
        session.state,
        networkMonitor.online,
        cart.count,
        combine(session.counts, content.config, events.maintenance) { counts, config, maintenance ->
            Triple(counts.unreadNotifications, config, maintenance)
        },
    ) { theme, sessionState, online, cartCount, (unread, config, maintenance) ->
        AppUiState(
            ready = sessionState != SessionState.Unknown,
            theme = theme, session = sessionState, online = online, cartCount = cartCount,
            unreadNotifications = unread, config = config, maintenance = maintenance,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    /** رویداد «نشست منقضی شد» برای نمایش پیام و هدایت به ورود. */
    val sessionExpired = events.expired

    /** هر بار که اپ به پیش‌زمینه می‌آید: تازه‌سازی دادهٔ حساب. */
    fun onAppResumed() {
        viewModelScope.launch {
            if (session.isSignedIn) {
                session.refreshMe()
                wishlist.sync()
            }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }
}
