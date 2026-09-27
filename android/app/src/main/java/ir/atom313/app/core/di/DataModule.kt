package ir.atom313.app.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.atom313.app.core.database.AtomDatabase
import ir.atom313.app.core.database.CacheDao
import ir.atom313.app.core.database.CartDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AtomDatabase =
        Room.databaseBuilder(context, AtomDatabase::class.java, "atom313.db")
            // فقط کش و سبد در پایگاه داده است؛ در تغییر اسکیمای آینده بدون مهاجرت، بازسازی بی‌خطر است
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun cacheDao(db: AtomDatabase): CacheDao = db.cacheDao()
    @Provides fun cartDao(db: AtomDatabase): CartDao = db.cartDao()

    /** اسکوپ سراسری برای کارهایی که نباید با بسته‌شدن صفحه لغو شوند (همگام‌سازی، خروج). */
    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
