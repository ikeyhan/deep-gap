package ir.atom313.app.core.network

import ir.atom313.app.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** افزودن توکن دسترسی به درخواست‌های API اصلی. */
class AuthInterceptor @Inject constructor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken
        val req = chain.request()
        if (token == null || req.header("Authorization") != null) return chain.proceed(req)
        return chain.proceed(req.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/** شناسایی حالت تعمیرات سرور از پاسخ‌ها (۵۰۳ با کد maintenance). */
class StatusInterceptor @Inject constructor(private val events: SessionEvents) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val res = chain.proceed(chain.request())
        if (res.code == 503 && res.peekBody(2048).string().contains("\"maintenance\"")) events.setMaintenance(true)
        else if (res.isSuccessful) events.setMaintenance(false)
        return res
    }
}

/** هدرهای شناسایی اپ (بدون اطلاعات حساس). */
class ClientHeadersInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .header("X-Device-Name", userAgent)
                .header("Accept", "application/json")
                .build(),
        )
}
