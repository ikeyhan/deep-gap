package ir.atom313.app.data.repository

import androidx.paging.PagingData
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.core.common.Outcome
import ir.atom313.app.core.common.map
import ir.atom313.app.core.database.CacheStore
import ir.atom313.app.core.di.AppScope
import ir.atom313.app.core.network.AtomApi
import ir.atom313.app.core.network.apiCall
import ir.atom313.app.core.network.dto.ArticleDto
import ir.atom313.app.core.network.dto.ConfigDto
import ir.atom313.app.core.network.dto.FaqDto
import ir.atom313.app.core.network.dto.ItemsDto
import ir.atom313.app.data.mapper.toDomain
import ir.atom313.app.data.paging.Page
import ir.atom313.app.data.paging.pager
import ir.atom313.app.domain.model.AppConfig
import ir.atom313.app.domain.model.Article
import ir.atom313.app.domain.model.Faq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** پیکربندی سایت، بلاگ و سؤالات متداول. */
@Singleton
class ContentRepository @Inject constructor(
    private val api: AtomApi,
    private val cache: CacheStore,
    @AppScope private val scope: CoroutineScope,
) {
    private val _config = MutableStateFlow(AppConfig())
    /** همیشه مقداری دارد (پیش‌فرض → کش → سرور). */
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    fun refreshConfig() {
        scope.launch {
            cache.read("config", ConfigDto.serializer())?.let { _config.value = it.toDomain() }
            when (val r = apiCall { api.config() }) {
                is Outcome.Success -> {
                    _config.value = r.value.toDomain()
                    cache.write("config", ConfigDto.serializer(), r.value, personal = false)
                }
                is Outcome.Failure -> Unit
            }
        }
    }

    fun articles(): Flow<PagingData<Article>> = pager(pageSize = 10) { page, limit ->
        apiCall { api.articles(page, limit) }.map { Page(it.items.map { a -> a.toDomain() }, it.hasMore) }
    }

    fun article(id: Long): Flow<LoadState<Article>> =
        cache.cached("article:$id", ArticleDto.serializer()) { apiCall { api.article(id).article } }
            .map { s -> LoadState(s.data?.toDomain(), s.loading, s.error, s.fromCache) }

    fun faqs(): Flow<LoadState<List<Faq>>> =
        cache.cached("faqs", ItemsDto.serializer(FaqDto.serializer())) { apiCall { api.faqs() } }
            .map { s -> LoadState(s.data?.items?.map { it.toDomain() }, s.loading, s.error, s.fromCache) }
}
