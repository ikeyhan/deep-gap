package ir.atom313.app.core.network

import ir.atom313.app.core.network.dto.RefreshRequest
import ir.atom313.app.core.security.StoredSession
import ir.atom313.app.core.security.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نوسازی خودکار توکن دسترسی هنگام ۴۰۱ «token_invalid».
 * - هم‌زمانی: فقط یک نوسازی در لحظه؛ درخواست‌های دیگر با توکن تازه تکرار می‌شوند.
 * - refresh رد شد → نشست پاک و رویداد «انقضای نشست» منتشر می‌شود.
 * - خطای شبکه هنگام نوسازی → IOException (کاربر خارج نمی‌شود؛ پیام آفلاین).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokens: TokenStore,
    private val authApi: AuthApi,
    private val events: SessionEvents,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (responseCount(response) >= 2) return null
        // فقط توکن منقضی/نامعتبر نوسازی می‌شود؛ «حساب غیرفعال» یا «نیاز به ورود» نه
        if (!response.peekBody(2048).string().contains("\"token_invalid\"")) return null

        synchronized(lock) {
            val current = tokens.accessToken ?: return null
            if (current != sent) return retry(response.request, current) // نخ دیگری نوسازی کرده است
            val refresh = tokens.refreshToken ?: return null

            val result = try {
                authApi.refreshBlocking(RefreshRequest(refresh)).execute()
            } catch (e: IOException) {
                throw e
            }
            val body = result.body()
            if (result.isSuccessful && body != null) {
                val user = body.user ?: tokens.session.value?.user ?: return null
                runBlocking { tokens.save(StoredSession(body.accessToken, body.refreshToken, user)) }
                return retry(response.request, body.accessToken)
            }
            if (result.code() == 401 || result.code() == 403) {
                runBlocking { tokens.clear() }
                events.notifyExpired()
            }
            return null
        }
    }

    private fun retry(request: Request, token: String) =
        request.newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var n = 1
        while (r?.priorResponse != null) { n++; r = r.priorResponse }
        return n
    }
}
