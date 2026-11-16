package com.example.spacenews.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spacenews.SpaceNewsApplication
import com.example.spacenews.data.local.BookmarkRepository
import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import com.example.spacenews.data.model.NewsInterest
import com.example.spacenews.data.preferences.ThemePreferences
import com.example.spacenews.data.remote.SpaceflightNewsApiFactory
import com.example.spacenews.data.repository.ArticlePage
import com.example.spacenews.data.repository.NewsRepository
import com.example.spacenews.data.repository.mergeArticles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.spacenews.R
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

/** One state model for initial load, refresh, pagination, and retry. */
data class FeedUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextPage: String? = null,
    val pageError: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && !isRefreshing && articles.isEmpty() && error == null
    val canLoadMore: Boolean get() = nextPage != null
}
data class ArticleDetailsUiState(val article: Article, val isLoading: Boolean = false)

class NewsViewModel(
    application: Application,
    private val repository: NewsRepository = NewsRepository(SpaceflightNewsApiFactory.create(), application.getString(R.string.publisher_fallback)),
    private val bookmarkRepository: BookmarkRepository = (application as SpaceNewsApplication).bookmarkRepository,
    private val preferences: ThemePreferences = (application as SpaceNewsApplication).themePreferences
) : AndroidViewModel(application) {
    private val _home = MutableStateFlow(FeedUiState())
    val homeState: StateFlow<FeedUiState> = _home.asStateFlow()
    private val _explore = MutableStateFlow(FeedUiState(isLoading = false))
    val exploreState: StateFlow<FeedUiState> = _explore.asStateFlow()
    private val _forYou = MutableStateFlow(FeedUiState())
    val forYouState: StateFlow<FeedUiState> = _forYou.asStateFlow()
    private val _selectedCategory = MutableStateFlow(NewsCategory.NEWS)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val interests = preferences.interests
    val bookmarks: StateFlow<List<Article>> = bookmarkRepository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bookmarkedIds: StateFlow<Set<Int>> = bookmarkRepository.bookmarkIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    private val _articleDetails = MutableStateFlow<ArticleDetailsUiState?>(null)
    val articleDetails: StateFlow<ArticleDetailsUiState?> = _articleDetails.asStateFlow()

    private val bookmarkMutex = Mutex()
    private var homeJob: Job? = null
    private var homePageJob: Job? = null
    private var exploreJob: Job? = null
    private var exploreDebounce: Job? = null
    private var explorePageJob: Job? = null
    private var forYouJob: Job? = null
    private var detailsJob: Job? = null
    private var exploreToken = 0L
    private var forYouToken = 0L
    private var interestPages: Map<NewsInterest, String?> = emptyMap()

    init { refreshHome() }

    fun refreshHome() {
        if (homeJob?.isActive == true) return
        homePageJob?.cancel()
        val old = _home.value
        _home.value = old.copy(isLoading = old.articles.isEmpty(), isRefreshing = old.articles.isNotEmpty(), error = null, pageError = null)
        homeJob = viewModelScope.launch {
            try { val page = repository.fetchHome(); _home.value = FeedUiState(page.articles, isLoading = false, nextPage = page.next) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _home.value = if (old.articles.isNotEmpty()) old.copy(isLoading = false, isRefreshing = false, error = null, pageError = e.userMessage()) else old.copy(isLoading = false, isRefreshing = false, error = e.userMessage()) }
        }
    }
    fun loadMoreHome() {
        val old = _home.value; val url = old.nextPage ?: return
        if (old.isLoadingMore) return
        _home.value = old.copy(isLoadingMore = true, pageError = null)
        homePageJob = viewModelScope.launch {
            try { val page = repository.fetchPage(url); _home.value = _home.value.copy(articles = mergeArticles(old.articles, page.articles), isLoadingMore = false, nextPage = page.next) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _home.value = _home.value.copy(isLoadingMore = false, pageError = e.userMessage()) }
        }
    }
    fun retryHomePage() = loadMoreHome()

    fun setQuery(value: String) {
        _query.value = value
        exploreDebounce?.cancel()
        exploreJob?.cancel(); explorePageJob?.cancel()
        val token = ++exploreToken
        _explore.value = FeedUiState(isLoading = true)
        exploreDebounce = viewModelScope.launch {
            delay(350)
            exploreJob = viewModelScope.launch { runExplore(token) }
        }
    }
    fun selectCategory(category: NewsCategory) {
        _selectedCategory.value = category
        exploreDebounce?.cancel(); exploreJob?.cancel(); explorePageJob?.cancel()
        val token = ++exploreToken
        _explore.value = FeedUiState(isLoading = true)
        exploreJob = viewModelScope.launch { runExplore(token) }
    }
    private suspend fun runExplore(token: Long) {
        try {
            val page = repository.searchArticles(_query.value, _selectedCategory.value)
            if (token == exploreToken) _explore.value = FeedUiState(page.articles, isLoading = false, nextPage = page.next)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (token == exploreToken) _explore.value = FeedUiState(isLoading = false, error = e.userMessage())
        }
    }
    fun refreshExplore() {
        if (exploreJob?.isActive == true) return
        explorePageJob?.cancel()
        val old = _explore.value; val token = ++exploreToken
        _explore.value = old.copy(isLoading = old.articles.isEmpty(), isRefreshing = old.articles.isNotEmpty(), error = null, pageError = null)
        exploreJob = viewModelScope.launch {
            try { val page = repository.searchArticles(_query.value, _selectedCategory.value); if (token == exploreToken) _explore.value = FeedUiState(page.articles, isLoading = false, nextPage = page.next) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (token == exploreToken) _explore.value = if (old.articles.isNotEmpty()) old.copy(isRefreshing = false, error = null, pageError = e.userMessage()) else old.copy(isLoading = false, error = e.userMessage()) }
        }
    }
    fun loadMoreExplore() {
        val old = _explore.value; val url = old.nextPage ?: return
        if (old.isLoadingMore) return
        val token = exploreToken
        _explore.value = old.copy(isLoadingMore = true, pageError = null)
        explorePageJob = viewModelScope.launch {
            try {
                val page = repository.fetchPage(url, _selectedCategory.value)
                if (token == exploreToken) _explore.value = _explore.value.copy(articles = mergeArticles(old.articles, page.articles), isLoadingMore = false, nextPage = page.next)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (token == exploreToken) _explore.value = _explore.value.copy(isLoadingMore = false, pageError = e.userMessage()) }
        }
    }

    fun setInterests(selected: Set<NewsInterest>) {
        forYouJob?.cancel(); forYouToken++; interestPages = emptyMap()
        preferences.setInterests(selected)
        refreshForYou()
    }
    fun refreshForYou() {
        if (forYouJob?.isActive == true) return
        val old = _forYou.value; val token = ++forYouToken
        _forYou.value = old.copy(isLoading = old.articles.isEmpty(), isRefreshing = old.articles.isNotEmpty(), error = null, pageError = null)
        forYouJob = viewModelScope.launch {
            val selected = interests.value
            if (selected.isEmpty()) { interestPages = emptyMap(); _forYou.value = FeedUiState(isLoading = false); return@launch }
            try {
                val outcomes = selected.map { interest -> async { runCatching { interest to repository.fetchInterest(interest.searchTerm) } } }.awaitAll()
                if (token != forYouToken) return@launch
                val pages = outcomes.mapNotNull { it.getOrNull() }
                val errors = outcomes.count { it.isFailure }
                if (pages.isNotEmpty()) interestPages = pages.associate { it.first to it.second.next }
                val articles = pages.flatMap { it.second.articles }
                val combined = if (errors == selected.size && old.articles.isNotEmpty()) old.articles else mergeArticles(old.articles, articles)
                val message = getApplication<Application>().getString(R.string.for_you_partial_error, errors)
                _forYou.value = FeedUiState(combined, isLoading = false, nextPage = if (interestPages.values.any { it != null }) "interests" else null, pageError = if (errors > 0 && combined.isNotEmpty()) message else null, error = if (errors > 0 && combined.isEmpty()) getApplication<Application>().getString(R.string.error_network) else null)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (token == forYouToken) _forYou.value = old.copy(isLoading = false, isRefreshing = false, error = e.userMessage()) }
        }
    }
    fun loadMoreForYou() {
        if (_forYou.value.isLoadingMore || interestPages.values.none { it != null }) return
        val pending = interestPages.filterValues { it != null }
        val old = _forYou.value; _forYou.value = old.copy(isLoadingMore = true, pageError = null)
        viewModelScope.launch {
            val results = pending.map { (interest, url) -> async { interest to runCatching { repository.fetchPage(url!!) } } }.awaitAll()
            val successful = results.mapNotNull { (interest, result) -> result.getOrNull()?.let { interest to it } }
            successful.forEach { (interest, page) -> interestPages = interestPages + (interest to page.next) }
            val merged = mergeArticles(old.articles, successful.flatMap { it.second.articles })
            val failed = results.size - successful.size
            _forYou.value = old.copy(articles = merged, isLoading = false, isRefreshing = false, isLoadingMore = false, nextPage = if (interestPages.values.any { it != null }) "interests" else null, pageError = if (failed > 0) getApplication<Application>().getString(R.string.for_you_partial_error, failed) else null)
        }
    }
    fun loadArticleDetails(article: Article) {
        detailsJob?.cancel(); _articleDetails.value = ArticleDetailsUiState(article, isLoading = true)
        detailsJob = viewModelScope.launch {
            try {
                if (bookmarkRepository.isSaved(article.id)) { _articleDetails.value = ArticleDetailsUiState(article); return@launch }
                val latest = repository.fetchArticle(article.id).copy(category = article.category)
                _articleDetails.value = ArticleDetailsUiState(latest)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _articleDetails.value = ArticleDetailsUiState(article) }
        }
    }
    fun clearArticleDetails() { detailsJob?.cancel(); _articleDetails.value = null }
    fun toggleBookmark(article: Article) {
        viewModelScope.launch { bookmarkMutex.withLock { if (bookmarkRepository.isSaved(article.id)) bookmarkRepository.remove(article.id) else bookmarkRepository.save(article) } }
    }
    private fun loadMore(target: MutableStateFlow<FeedUiState>, fetch: suspend (String, NewsCategory) -> ArticlePage, category: NewsCategory = NewsCategory.NEWS) {
        val old = target.value; val url = old.nextPage ?: return
        if (old.isLoadingMore) return
        target.value = old.copy(isLoadingMore = true, pageError = null)
        viewModelScope.launch {
            try { val page = fetch(url, category); target.value = target.value.copy(articles = mergeArticles(old.articles, page.articles), isLoading = false, isLoadingMore = false, nextPage = page.next, pageError = null) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { target.value = target.value.copy(isLoadingMore = false, pageError = e.userMessage()) }
        }
    }
    private fun Exception.userMessage(): String = when (this) {
        is SocketTimeoutException -> getApplication<Application>().getString(R.string.error_timeout)
        is IOException -> getApplication<Application>().getString(R.string.error_network)
        is HttpException -> getApplication<Application>().getString(R.string.error_http, code())
        else -> getApplication<Application>().getString(R.string.error_generic)
    }
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(NewsViewModel::class.java)) NewsViewModel(application) as T else throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
