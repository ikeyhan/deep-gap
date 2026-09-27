package ir.atom313.app.core.network

import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.network.dto.ErrorEnvelope
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** تبدیل هر خطای شبکه/سرور به [AppError] قابل‌نمایش؛ هرگز پیام فنی به کاربر نمی‌رسد. */
object ErrorMapper {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun fromHttp(code: Int, rawBody: String?): AppError {
        val body = rawBody?.let { runCatching { json.decodeFromString(ErrorEnvelope.serializer(), it).error }.getOrNull() }
        val msg = body?.message?.takeIf { it.isNotBlank() }
        val errCode = body?.code ?: ""
        return when {
            code == 503 && errCode == "maintenance" -> AppError.Maintenance(msg ?: AppError.Maintenance().message)
            code == 400 || code == 422 -> AppError.Validation(errCode, msg ?: "اطلاعات واردشده نامعتبر است.", body?.fields.orEmpty())
            code == 401 -> if (errCode == "invalid_credentials" || errCode == "wrong_password") {
                AppError.Validation(errCode, msg ?: "اطلاعات ورود نادرست است.", body?.fields.orEmpty())
            } else {
                AppError.Unauthorized(msg ?: AppError.Unauthorized().message)
            }
            code == 403 -> AppError.Forbidden(errCode, msg ?: "به این بخش دسترسی ندارید.")
            code == 404 -> AppError.NotFound(msg ?: AppError.NotFound().message)
            code == 409 -> AppError.Conflict(errCode, msg ?: "این درخواست با وضعیت فعلی سازگار نیست.")
            code == 429 -> AppError.RateLimited(msg ?: AppError.RateLimited().message)
            code >= 500 -> AppError.Server()
            else -> AppError.Unknown()
        }
    }

    fun fromThrowable(t: Throwable): AppError = when (t) {
        is HttpException -> fromHttp(t.code(), runCatching { t.response()?.errorBody()?.string() }.getOrNull())
        is SocketTimeoutException -> AppError.Timeout
        is UnknownHostException, is ConnectException -> AppError.Offline
        is SSLException -> AppError.Server("اتصال امن با سرور برقرار نشد.")
        is InterruptedIOException -> AppError.Timeout
        is IOException -> AppError.Offline
        is SerializationException -> AppError.Server("پاسخ سرور قابل‌خواندن نبود. اپ را به‌روز کنید.")
        else -> AppError.Unknown()
    }
}

/** اجرای امن فراخوانی API. لغو کوروتین دوباره پرتاب می‌شود. */
suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Outcome.Failure(ErrorMapper.fromThrowable(e))
}
