package ir.atom313.app.core.common

/**
 * خطاهای قابل‌نمایش به کاربر. پیام فنی سرور یا استک‌تریس هرگز به UI نمی‌رسد؛
 * فقط [message] فارسی که یا از سرور (فیلد error.message) آمده یا پیام پیش‌فرض است.
 */
sealed class AppError(open val message: String) {
    /** اتصال اینترنت برقرار نیست. */
    data object Offline : AppError("اتصال اینترنت برقرار نیست.")

    /** سرور در زمان مناسب پاسخ نداد. */
    data object Timeout : AppError("پاسخ سرور طول کشید. دوباره تلاش کنید.")

    /** نشست منقضی شده و باید دوباره وارد شد. */
    data class Unauthorized(override val message: String = "نشست شما منقضی شده است. دوباره وارد شوید.") : AppError(message)

    /** دسترسی مجاز نیست (مثلاً حساب مدیریتی در اپ مشتری). */
    data class Forbidden(val code: String, override val message: String) : AppError(message)

    /** سایت در حال به‌روزرسانی است. */
    data class Maintenance(override val message: String = "اتم ۳۱۳ در حال به‌روزرسانی است. کمی بعد دوباره سر بزنید.") : AppError(message)

    /** خطای ورودی با پیام‌های هر فیلد. */
    data class Validation(
        val code: String,
        override val message: String,
        val fields: Map<String, String> = emptyMap(),
    ) : AppError(message)

    data class NotFound(override val message: String = "مورد درخواستی یافت نشد.") : AppError(message)

    data class Conflict(val code: String, override val message: String) : AppError(message)

    data class RateLimited(override val message: String = "درخواست‌های زیادی ارسال شد. کمی بعد تلاش کنید.") : AppError(message)

    data class Server(override val message: String = "خطایی در سرور رخ داد. لطفاً دوباره تلاش کنید.") : AppError(message)

    data class Unknown(override val message: String = "خطای غیرمنتظره‌ای رخ داد. دوباره تلاش کنید.") : AppError(message)

    /** آیا با تلاش دوباره ممکن است برطرف شود؟ */
    val isRetryable: Boolean
        get() = this is Offline || this is Timeout || this is Server || this is Unknown || this is RateLimited
}

/** نتیجهٔ عملیات لایهٔ داده. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> = also { if (it is Outcome.Success) block(it.value) }
inline fun <T> Outcome<T>.onFailure(block: (AppError) -> Unit): Outcome<T> = also { if (it is Outcome.Failure) block(it.error) }
fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value
