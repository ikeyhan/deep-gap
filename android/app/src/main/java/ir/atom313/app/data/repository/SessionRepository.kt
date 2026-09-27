package ir.atom313.app.data.repository

import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.database.CacheStore
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.core.di.AppScope
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.AuthApi
import ir.atom313.app.core.network.SessionEvents
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.AuthResponse
import ir.atom313.app.core.network.dto.LoginRequest
import ir.atom313.app.core.network.dto.LogoutRequest
import ir.atom313.app.core.network.dto.RegisterRequest
import ir.atom313.app.core.security.StoredSession
import ir.atom313.app.core.security.TokenStore
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.domain.model.AccountCounts
import ir.atom313.app.domain.model.SessionState
import ir.atom313.app.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RegisterInput(
    val name: String,
    val phone: String,
    val username: String,
    val password: String,
    val email: String = "",
    val city: String = "",
)

/**
 * منبع حقیقت وضعیت ورود. همان حساب‌های سایت؛ نشست به‌صورت رمزشده در TokenStore است.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val authApi: AuthApi,
    private val api: AtomApi,
    private val tokens: TokenStore,
    private val cache: CacheStore,
    private val prefs: UserPreferences,
    private val events: SessionEvents,
    @AppScope private val scope: CoroutineScope,
) {
    private val loaded = MutableStateFlow(false)

    val state: StateFlow<SessionState> = combine(loaded, tokens.session) { isLoaded, s ->
        when {
            !isLoaded -> SessionState.Unknown
            s == null -> SessionState.Guest
            else -> SessionState.SignedIn(s.user.toDomain())
        }
    }.stateIn(scope, SharingStarted.Eagerly, SessionState.Unknown)

    private val _counts = MutableStateFlow(AccountCounts())
    val counts: StateFlow<AccountCounts> = _counts.asStateFlow()

    val currentUser: User? get() = (state.value as? SessionState.SignedIn)?.user
    val isSignedIn: Boolean get() = tokens.session.value != null

    init {
        // نشست باطل‌شده از سمت سرور → پاک‌سازی دادهٔ شخصی
        scope.launch { events.expired.collect { clearLocalAccountData() } }
    }

    suspend fun initialize() {
        tokens.ensureLoaded()
        loaded.value = true
    }

    suspend fun login(username: String, password: String): Outcome<User> {
        val u = username.trim().let { if (Validators.isMobile(it)) Validators.normalizeMobile(it) else it.lowercase() }
        return when (val r = apiCall { authApi.login(LoginRequest(u, password)) }) {
            is Outcome.Success -> persist(r.value)
            is Outcome.Failure -> r
        }
    }

    suspend fun register(input: RegisterInput): Outcome<User> {
        val req = RegisterRequest(
            name = input.name.trim(), phone = Validators.normalizeMobile(input.phone),
            username = input.username.trim().lowercase(), password = input.password,
            email = input.email.trim(), city = input.city.trim(),
        )
        return when (val r = apiCall { authApi.register(req) }) {
            is Outcome.Success -> persist(r.value)
            is Outcome.Failure -> r
        }
    }

    private suspend fun persist(res: AuthResponse): Outcome<User> {
        val user = res.user ?: return Outcome.Failure(AppError.Server())
        tokens.save(StoredSession(res.accessToken, res.refreshToken, user))
        return Outcome.Success(user.toDomain())
    }

    /** پس از تغییر رمز، سرور جفت توکن تازه می‌دهد (بقیهٔ نشست‌ها باطل شده‌اند). */
    suspend fun replaceTokens(res: AuthResponse) {
        val user = res.user ?: tokens.session.value?.user ?: return
        tokens.save(StoredSession(res.accessToken, res.refreshToken, user))
    }

    /** پروفایل و شمارنده‌ها را از سرور تازه می‌کند (همگام با تغییرات سایت). */
    suspend fun refreshMe(): Outcome<User> = when (val r = apiCall { api.me() }) {
        is Outcome.Success -> {
            tokens.updateUser(r.value.user)
            _counts.value = r.value.counts.toDomain()
            Outcome.Success(r.value.user.toDomain())
        }
        is Outcome.Failure -> r
    }

    suspend fun updateStoredUser(user: ir.atom313.app.core.network.dto.UserDto) = tokens.updateUser(user)

    fun setUnreadCount(n: Int) { _counts.value = _counts.value.copy(unreadNotifications = n) }

    /** خروج: ابطال refresh روی سرور (در صورت امکان) و پاک‌سازی کامل دادهٔ حساب از دستگاه. */
    suspend fun logout(deviceToken: String?) {
        val refresh = tokens.refreshToken
        if (refresh != null) apiCall { authApi.logout(LogoutRequest(refresh, deviceToken)) }
        clearLocalAccountData()
    }

    suspend fun clearLocalAccountData() {
        tokens.clear()
        cache.clearPersonal()
        prefs.clearAccountData()
        _counts.value = AccountCounts()
    }
}
