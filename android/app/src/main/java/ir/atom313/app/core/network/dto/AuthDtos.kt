package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String,
    val username: String,
    val password: String,
    val email: String = "",
    val city: String = "",
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String, val deviceToken: String? = null)

@Serializable
data class ChangePasswordRequest(val current: String, val next: String)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val address: String = "",
    val role: String = "customer",
    val storeName: String = "",
    val createdAt: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 900,
    val user: UserDto? = null,
)

@Serializable
data class MeResponse(val user: UserDto, val counts: CountsDto = CountsDto())

@Serializable
data class CountsDto(val unreadNotifications: Int = 0, val orders: Int = 0, val wishlist: Int = 0)

@Serializable
data class UserEnvelope(val user: UserDto)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val phone: String,
    val email: String,
    val city: String,
    val address: String,
)

@Serializable
data class DeleteAccountRequest(val password: String)
