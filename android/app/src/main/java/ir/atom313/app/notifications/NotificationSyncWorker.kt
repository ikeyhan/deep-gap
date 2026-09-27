package ir.atom313.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.data.repository.NotificationRepository
import ir.atom313.app.data.repository.SessionRepository

/**
 * همگام‌سازی دوره‌ای اعلان‌ها (هر ۳۰ دقیقه، فقط با اینترنت). سبک است: یک درخواست کوچک
 * «جدیدتر از آخرین شناسه». جایگزین بی‌نیاز از سرویس‌های گوگل برای پوش؛ با FCM هم سازگار می‌ماند.
 */
@HiltWorker
class NotificationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val session: SessionRepository,
    private val repository: NotificationRepository,
    private val publisher: NotificationPublisher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        session.initialize()
        if (!session.isSignedIn) return Result.success()
        return when (val r = repository.fetchNew()) {
            is Outcome.Success -> { r.value.take(5).forEach(publisher::show); Result.success() }
            is Outcome.Failure -> if (r.error.isRetryable && r.error !is AppError.RateLimited) Result.retry() else Result.success()
        }
    }
}
