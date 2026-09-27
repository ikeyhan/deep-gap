package ir.atom313.app.data.repository

import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.Validators
import ir.atom313.app.core.common.map
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.ChangePasswordRequest
import ir.atom313.app.core.network.dto.DeleteAccountRequest
import ir.atom313.app.core.network.dto.UpdateProfileRequest
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileInput(val name: String, val phone: String, val email: String, val city: String, val address: String)

/** پروفایل، رمز عبور و حذف حساب — همه روی همان حساب سایت. */
@Singleton
class AccountRepository @Inject constructor(
    private val api: AtomApi,
    private val session: SessionRepository,
) {
    suspend fun updateProfile(input: ProfileInput): Outcome<User> {
        val r = apiCall {
            api.updateMe(
                UpdateProfileRequest(
                    name = input.name.trim(), phone = Validators.normalizeMobile(input.phone),
                    email = input.email.trim(), city = input.city.trim(), address = input.address.trim(),
                ),
            )
        }
        return when (r) {
            is Outcome.Success -> { session.updateStoredUser(r.value.user); Outcome.Success(r.value.user.toDomain()) }
            is Outcome.Failure -> r
        }
    }

    suspend fun changePassword(current: String, next: String): Outcome<Unit> =
        when (val r = apiCall { api.changePassword(ChangePasswordRequest(current, next)) }) {
            is Outcome.Success -> { session.replaceTokens(r.value); Outcome.Success(Unit) }
            is Outcome.Failure -> r
        }

    suspend fun deleteAccount(password: String): Outcome<Unit> =
        apiCall { api.deleteMe(DeleteAccountRequest(password)) }.map { }
}
