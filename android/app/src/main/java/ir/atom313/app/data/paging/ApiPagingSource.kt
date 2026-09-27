package ir.atom313.app.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import ir.atom313.app.core.common.AppError
import ir.atom313.app.core.common.Outcome
import kotlinx.coroutines.flow.Flow

/** صفحهٔ دریافتی از API (مستقل از DTO). */
data class Page<T>(val items: List<T>, val hasMore: Boolean)

/** خطای صفحه‌بندی که [AppError] قابل‌نمایش را با خود دارد. */
class PagingLoadException(val error: AppError) : Exception(error.message)

/** منبع صفحه‌بندی عمومی برای همهٔ فهرست‌های API (صفحه از ۱ شروع می‌شود). */
class ApiPagingSource<T : Any>(
    private val fetch: suspend (page: Int, limit: Int) -> Outcome<Page<T>>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return when (val r = fetch(page, params.loadSize.coerceAtMost(50))) {
            is Outcome.Success -> LoadResult.Page(
                data = r.value.items,
                prevKey = null,
                nextKey = if (r.value.hasMore) page + 1 else null,
            )
            is Outcome.Failure -> LoadResult.Error(PagingLoadException(r.error))
        }
    }
}

fun <T : Any> pager(pageSize: Int = 20, fetch: suspend (page: Int, limit: Int) -> Outcome<Page<T>>): Flow<PagingData<T>> =
    Pager(
        config = PagingConfig(pageSize = pageSize, initialLoadSize = pageSize, prefetchDistance = pageSize / 2, enablePlaceholders = false),
        pagingSourceFactory = { ApiPagingSource(fetch) },
    ).flow

/** نگاشت خطای Paging به AppError برای نمایش. */
fun Throwable.toAppError(): AppError = (this as? PagingLoadException)?.error ?: AppError.Unknown()
