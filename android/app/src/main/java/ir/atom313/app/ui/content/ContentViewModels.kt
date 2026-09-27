package ir.atom313.app.ui.content

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.atom313.app.core.common.LoadState
import ir.atom313.app.data.repository.ContentRepository
import ir.atom313.app.domain.model.Article
import ir.atom313.app.domain.model.Faq
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BlogViewModel @Inject constructor(content: ContentRepository) : ViewModel() {
    val articles: Flow<PagingData<Article>> = content.articles().cachedIn(viewModelScope)
}

@HiltViewModel
class ArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
) : ViewModel() {
    private val id: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L
    private val reload = MutableStateFlow(0)

    val state: StateFlow<LoadState<Article>> = reload
        .flatMapLatest { content.article(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    fun retry() { reload.value++ }
}

@HiltViewModel
class FaqViewModel @Inject constructor(private val content: ContentRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)
    val state: StateFlow<LoadState<List<Faq>>> = reload
        .flatMapLatest { content.faqs() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.loading())

    fun retry() { reload.value++ }
}
