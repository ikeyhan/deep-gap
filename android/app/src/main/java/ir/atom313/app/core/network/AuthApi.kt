package ir.atom313.app.core.network

import ir.atom313.app.core.network.dto.AuthResponse
import ir.atom313.app.core.network.dto.LoginRequest
import ir.atom313.app.core.network.dto.LogoutRequest
import ir.atom313.app.core.network.dto.OkDto
import ir.atom313.app.core.network.dto.RefreshRequest
import ir.atom313.app.core.network.dto.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** مسیرهای احراز هویت — روی کلاینتی بدون Authenticator تا نوسازی توکن به حلقه نیفتد. */
interface AuthApi {
    @POST("v1/auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("v1/auth/register") suspend fun register(@Body body: RegisterRequest): AuthResponse
    @POST("v1/auth/logout") suspend fun logout(@Body body: LogoutRequest): OkDto

    /** همگام (Call) چون از داخل OkHttp Authenticator روی نخ شبکه صدا زده می‌شود. */
    @POST("v1/auth/refresh") fun refreshBlocking(@Body body: RefreshRequest): Call<AuthResponse>
}
