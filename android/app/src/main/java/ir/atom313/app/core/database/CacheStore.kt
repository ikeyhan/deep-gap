package ir.atom313.app.core.database

import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * الگوی stale-while-revalidate:
 * ۱) اگر کش هست فوراً نمایش بده، ۲) از شبکه بگیر، ۳) کش را به‌روز کن.
 * اگر شبکه شکست بخورد و کش داشته باشیم، همان کش با خطای «کهنه» نمایش داده می‌شود.
 */
@Singleton
class CacheStore @Inject constructor(
    private val dao: CacheDao,
    private val json: Json,
) {
    fun <T> cached(
        key: String,
        serializer: KSerializer<T>,
        personal: Boolean = false,
        fetch: suspend () -> Outcome<T>,
    ): Flow<LoadState<T>> = flow {
        val cached = dao.get(key)?.let { runCatching { json.decodeFromString(serializer, it.json) }.getOrNull() }
        emit(LoadState(data = cached, loading = true, fromCache = cached != null))
        when (val r = fetch()) {
            is Outcome.Success -> {
                dao.put(CacheEntity(key, json.encodeToString(serializer, r.value), System.currentTimeMillis(), personal))
                emit(LoadState(data = r.value))
            }
            is Outcome.Failure -> emit(LoadState(data = cached, error = r.error, fromCache = cached != null))
        }
    }

    suspend fun <T> read(key: String, serializer: KSerializer<T>): T? =
        dao.get(key)?.let { runCatching { json.decodeFromString(serializer, it.json) }.getOrNull() }

    suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T, personal: Boolean) =
        dao.put(CacheEntity(key, json.encodeToString(serializer, value), System.currentTimeMillis(), personal))

    suspend fun clearPersonal() = dao.clearPersonal()

    /** حذف کش‌های قدیمی‌تر از ۳۰ روز */
    suspend fun prune() = dao.prune(System.currentTimeMillis() - 30L * 24 * 3600 * 1000)

}
