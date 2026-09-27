package ir.atom313.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.datastore.ThemeMode
import ir.atom313.app.core.datastore.UserPreferences
import ir.atom313.app.data.repository.AccountRepository
import ir.atom313.app.data.repository.NotificationRepository
import ir.atom313.app.data.repository.ProfileInput
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.data.repository.WishlistRepository
import ir.atom313.app.domain.model.AccountCounts
import ir.atom313.app.domain.model.AppNotification
import ir.atom313.app.domain.model.Product
import ir.atom313.app.domain.model.SessionState
import ir.atom313.app.domain.model.User
import ir.atom313.app.domain.usecase.LogoutUseCase
import ir.atom313.app.ui.cart.CartActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val session: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val prefs: UserPreferences,
) : ViewModel() {

    val user: StateFlow<User?> = session.state
        .map { (it as? SessionState.SignedIn)?.user }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val counts: StateFlow<AccountCounts> = session.counts
    val theme: StateFlow<ThemeMode> = prefs.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun refresh() = viewModelScope.launch { if (session.isSignedIn) session.refreshMe() }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }
    fun logout(onDone: () -> Unit) = viewModelScope.launch { logoutUseCase(); onDone() }
}

data class ProfileUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val address: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        session.currentUser?.let { u ->
            _state.value = ProfileUiState(u.name, u.phone, u.email, u.city, u.address)
        }
        viewModelScope.launch {
            if (session.refreshMe() is Outcome.Success) {
                session.currentUser?.let { u ->
                    if (!_state.value.saving) _state.value = _state.value.copy(
                        name = u.name, phone = u.phone, email = u.email, city = u.city, address = u.address,
                    )
                }
            }
        }
    }

    fun update(transform: (ProfileUiState) -> ProfileUiState) {
        _state.value = transform(_state.value).copy(errors = emptyMap(), generalError = null, saved = false)
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            when (val r = account.updateProfile(ProfileInput(s.name, s.phone, s.email, s.city, s.address))) {
                is Outcome.Success -> _state.value = _state.value.copy(saving = false, saved = true)
                is Outcome.Failure -> _state.value = _state.value.copy(
                    saving = false,
                    errors = (r.error as? AppError.Validation)?.fields.orEmpty(),
                    generalError = if (r.error is AppError.Validation && r.error.fields.isNotEmpty()) null else r.error.message,
                )
            }
        }
    }
}

data class SecurityUiState(
    val current: String = "",
    val next: String = "",
    val confirm: String = "",
    val saving: Boolean = false,
    val changed: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val deletePassword: String = "",
    val deleting: Boolean = false,
    val deleteError: String? = null,
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityUiState())
    val state: StateFlow<SecurityUiState> = _state.asStateFlow()

    fun update(transform: (SecurityUiState) -> SecurityUiState) {
        _state.value = transform(_state.value).copy(errors = emptyMap(), generalError = null, deleteError = null, changed = false)
    }

    fun changePassword() {
        val s = _state.value
        val errors = buildMap {
            if (s.current.isEmpty()) put("current", "رمز فعلی را وارد کنید.")
            if (s.next.length < 8) put("next", "رمز جدید باید حداقل ۸ نویسه باشد.")
            if (s.next != s.confirm) put("confirm", "تکرار رمز جدید مطابقت ندارد.")
        }
        if (errors.isNotEmpty()) { _state.value = s.copy(errors = errors); return }
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            when (val r = account.changePassword(s.current, s.next)) {
                is Outcome.Success -> _state.value = SecurityUiState(changed = true)
                is Outcome.Failure -> _state.value = _state.value.copy(
                    saving = false,
                    errors = (r.error as? AppError.Validation)?.fields.orEmpty(),
                    generalError = if (r.error is AppError.Validation && r.error.fields.isNotEmpty()) null else r.error.message,
                )
            }
        }
    }

    /** حذف حساب (الزام فروشگاه گوگل پلی) — پس از حذف، نشست محلی پاک می‌شود. */
    fun deleteAccount(onDone: () -> Unit) {
        val s = _state.value
        if (s.deletePassword.isEmpty()) { _state.value = s.copy(deleteError = "برای تأیید، رمز عبور را وارد کنید."); return }
        viewModelScope.launch {
            _state.value = s.copy(deleting = true, deleteError = null)
            when (val r = account.deleteAccount(s.deletePassword)) {
                is Outcome.Success -> { session.clearLocalAccountData(); onDone() }
                is Outcome.Failure -> _state.value = _state.value.copy(deleting = false, deleteError = r.error.message)
            }
        }
    }
}

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlist: WishlistRepository,
    private val cartActions: CartActions,
) : ViewModel() {

    private val reload = MutableStateFlow(0)
    private val _state = MutableStateFlow<LoadState<List<Product>>>(LoadState.loading())
    val state: StateFlow<LoadState<List<Product>>> = _state.asStateFlow()
    val ids: StateFlow<Set<Long>> = wishlist.ids

    init { load() }

    fun load() {
        viewModelScope.launch { wishlist.items().collect { _state.value = it } }
    }

    fun toggle(productId: Long, onMessage: (String) -> Unit) {
        viewModelScope.launch {
            when (val r = wishlist.toggle(productId)) {
                is Outcome.Success -> {
                    onMessage(if (r.value) "به علاقه‌مندی‌ها اضافه شد" else "از علاقه‌مندی‌ها حذف شد")
                    if (!r.value) _state.value = _state.value.copy(data = _state.value.data?.filterNot { it.id == productId })
                }
                is Outcome.Failure -> onMessage(r.error.message)
            }
        }
    }

    fun addToCart(product: Product, onMessage: (String) -> Unit) = viewModelScope.launch { cartActions.add(product, onMessage) }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<AppNotification>>>(LoadState.loading())
    val state: StateFlow<LoadState<List<AppNotification>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = LoadState.loading(_state.value.data)
            when (val r = repository.page(1, 30)) {
                is Outcome.Success -> _state.value = LoadState(r.value.items)
                is Outcome.Failure -> _state.value = LoadState(_state.value.data, error = r.error)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            _state.value = _state.value.copy(data = _state.value.data?.map { it.copy(read = true) })
            repository.markAllRead()
        }
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(data = _state.value.data?.map { if (it.id == id) it.copy(read = true) else it })
            repository.markRead(listOf(id))
        }
    }
}
