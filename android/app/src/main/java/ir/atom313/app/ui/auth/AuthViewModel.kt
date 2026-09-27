package ir.atom313.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.RegisterInput
import ir.atom313.app.domain.usecase.LoginUseCase
import ir.atom313.app.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val loading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val register: RegisterUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun update(transform: (AuthUiState) -> AuthUiState) {
        _state.value = transform(_state.value).copy(errors = emptyMap(), generalError = null)
    }

    fun signIn() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(loading = true, errors = emptyMap(), generalError = null)
            handle(login(s.username, s.password))
        }
    }

    fun signUp() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(loading = true, errors = emptyMap(), generalError = null)
            handle(register(RegisterInput(s.name, s.phone, s.username, s.password, s.email, s.city)))
        }
    }

    private fun handle(result: Outcome<*>) {
        _state.value = when (result) {
            is Outcome.Success -> _state.value.copy(loading = false, success = true)
            is Outcome.Failure -> {
                val error = result.error
                _state.value.copy(
                    loading = false,
                    errors = (error as? AppError.Validation)?.fields.orEmpty(),
                    // خطای فیلددار در همان فیلد نمایش داده می‌شود؛ بقیه به‌صورت پیام کلی
                    generalError = if (error is AppError.Validation && error.fields.isNotEmpty()) null else error.message,
                )
            }
        }
    }
}
