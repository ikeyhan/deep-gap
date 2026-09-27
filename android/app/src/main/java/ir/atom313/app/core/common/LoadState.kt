package ir.atom313.app.core.common

/**
 * وضعیت یک دادهٔ قابل‌بارگذاری با پشتیبانی از کش:
 * ممکن است هم‌زمان دادهٔ کش‌شده نمایش داده شود و بارگذاری تازه در جریان باشد
 * (stale-while-revalidate) یا به‌روزرسانی شکست خورده باشد اما دادهٔ قبلی بماند.
 */
data class LoadState<out T>(
    val data: T? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val fromCache: Boolean = false,
) {
    /** هیچ داده‌ای نداریم و در حال بارگذاری هستیم → اسکلتون نمایش بده. */
    val isInitialLoading: Boolean get() = data == null && loading
    /** داده‌ای نداریم و خطا رخ داده → صفحهٔ خطای کامل. */
    val isFullError: Boolean get() = data == null && !loading && error != null
    /** داده داریم ولی به‌روزرسانی شکست خورده (مثلاً آفلاین). */
    val isStale: Boolean get() = data != null && error != null

    companion object {
        fun <T> loading(previous: T? = null) = LoadState(data = previous, loading = true)
    }
}
