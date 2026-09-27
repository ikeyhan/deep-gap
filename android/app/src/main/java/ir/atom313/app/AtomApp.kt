package ir.atom313.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.atom313.app.core.database.CacheStore
import ir.atom313.app.core.di.AppScope
import ir.atom313.app.data.repository.ContentRepository
import ir.atom313.app.data.repository.SessionRepository
import ir.atom313.app.notifications.NotificationPublisher
import ir.atom313.app.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AtomApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var session: SessionRepository
    @Inject lateinit var content: ContentRepository
    @Inject lateinit var cache: CacheStore
    @Inject lateinit var publisher: NotificationPublisher
    @Inject lateinit var scheduler: NotificationScheduler
    @Inject @AppScope lateinit var scope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        publisher.createChannels()
        // کارهای سبکِ شروع، خارج از مسیر نمایش اولین فریم
        scope.launch {
            session.initialize()
            content.refreshConfig()
            cache.prune()
            if (session.isSignedIn) scheduler.schedule()
        }
    }
}
