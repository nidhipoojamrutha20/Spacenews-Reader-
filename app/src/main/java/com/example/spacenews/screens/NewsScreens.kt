package com.example.spacenews.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.spacenews.data.local.SavedSort
import com.example.spacenews.data.local.filterAndSortBookmarks
import com.example.spacenews.data.model.NewsInterest
import com.example.spacenews.data.preferences.ReadingSize
import com.example.spacenews.data.preferences.SpeechSpeed
import com.example.spacenews.ui.speech.ArticleSpeechController
import com.example.spacenews.ui.speech.SpeechState
import com.example.spacenews.ui.viewmodel.FeedUiState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacenews.R
import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import com.example.spacenews.data.preferences.ThemeMode
import com.example.spacenews.data.util.validatedArticleUrl
import com.example.spacenews.ui.components.ArticleImage
import com.example.spacenews.ui.viewmodel.ArticleDetailsUiState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: FeedUiState, bookmarks: Set<Int>, onRefresh: () -> Unit, onLoadMore: () -> Unit, onRetryPage: () -> Unit, onArticleClick: (Article) -> Unit, onBookmarkClick: (Article) -> Unit, onExploreClick: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.articles.size, state.nextPage, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { index ->
            if (state.canLoadMore && !state.isLoadingMore && index >= listState.layoutInfo.totalItemsCount - 4) onLoadMore()
        }
    }
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { HomeHeader() }
            item { SectionHeading(stringResource(R.string.latest_transmissions), stringResource(R.string.explore_action), onExploreClick) }
            when {
                state.isLoading -> item { LoadingState(stringResource(R.string.loading_home)) }
                state.error != null && state.articles.isEmpty() -> item { ErrorState(state.error, onRefresh) }
                state.articles.isEmpty() -> item { EmptyState(stringResource(R.string.empty_home_title), stringResource(R.string.empty_home_body)) }
                else -> items(state.articles, key = { it.id }) { article -> ArticleCard(article, article.id in bookmarks, { onArticleClick(article) }, { onBookmarkClick(article) }) }
            }
            item { FeedFooter(state, onLoadMore, onRetryPage) }
        }
    }
}

@Composable
private fun HomeHeader() {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(R.string.home_greeting), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(5.dp))
            Text(stringResource(R.string.home_headline), color = colors.onBackground, style = MaterialTheme.typography.headlineMedium, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        }
        Surface(shape = CircleShape, color = colors.surfaceVariant, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.NotificationsNone, null, tint = colors.primary) }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(action, color = colors.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp))
    }
}

@Composable
fun CategoriesScreen(state: FeedUiState, selectedCategory: NewsCategory, query: String, bookmarks: Set<Int>, onQueryChange: (String) -> Unit, onSelectCategory: (NewsCategory) -> Unit, onRetry: () -> Unit, onRefresh: () -> Unit, onLoadMore: () -> Unit, onRetryPage: () -> Unit, onArticleClick: (Article) -> Unit, onBookmarkClick: (Article) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.articles.size, state.nextPage, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { index -> if (state.canLoadMore && !state.isLoadingMore && index >= listState.layoutInfo.totalItemsCount - 4) onLoadMore() }
    }
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(stringResource(R.string.explore_title), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.explore_description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text(stringResource(R.string.search_articles)) }, singleLine = true)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    items(NewsCategory.entries, key = { it.name }) { category ->
                        val active = category == selectedCategory
                        FilterChip(selected = active, onClick = { onSelectCategory(category) }, label = { Text(stringResource(category.labelResource())) })
                    }
                }
            }
            when {
                state.isLoading -> item { LoadingState(stringResource(R.string.loading_topic, stringResource(selectedCategory.labelResource()))) }
                state.error != null && state.articles.isEmpty() -> item { ErrorState(state.error, onRetry) }
                state.articles.isEmpty() -> item { EmptyState(stringResource(R.string.empty_topic_title, stringResource(selectedCategory.labelResource())), stringResource(R.string.empty_topic_body)) }
                else -> items(state.articles, key = { it.id }) { article -> ArticleCard(article, article.id in bookmarks, { onArticleClick(article) }, { onBookmarkClick(article) }) }
            }
            item { FeedFooter(state, onLoadMore, onRetryPage) }
        }
    }
}

@Composable
fun ForYouScreen(state: FeedUiState, interests: Set<NewsInterest>, bookmarks: Set<Int>, onRefresh: () -> Unit, onLoadMore: () -> Unit, onRetry: () -> Unit, onArticleClick: (Article) -> Unit, onBookmarkClick: (Article) -> Unit, onEditInterests: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.articles.size, state.nextPage, state.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { index -> if (state.canLoadMore && !state.isLoadingMore && index >= listState.layoutInfo.totalItemsCount - 4) onLoadMore() }
    }
    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(stringResource(R.string.for_you_title), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.for_you_description), modifier = Modifier.padding(top = 6.dp), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(interests.joinToString(" · ") { it.name.lowercase().replaceFirstChar(Char::uppercase) }.ifBlank { stringResource(R.string.no_interests) }, modifier = Modifier.padding(top = 10.dp), color = colors.secondary, style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = onEditInterests, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.edit_interests)) }
            }
            when {
                state.isLoading -> item { LoadingState(stringResource(R.string.loading_home)) }
                state.error != null && state.articles.isEmpty() -> item { ErrorState(state.error, onRetry) }
                state.articles.isEmpty() -> item { EmptyState(stringResource(R.string.for_you_empty_title), stringResource(R.string.for_you_empty_body)) }
                else -> items(state.articles, key = { it.id }) { article -> ArticleCard(article, article.id in bookmarks, { onArticleClick(article) }, { onBookmarkClick(article) }) }
            }
            if (state.pageError != null) item { ErrorState(state.pageError, onRetry) }
            else item { FeedFooter(state, onLoadMore, onLoadMore) }
        }
    }
}

@Composable
fun BookmarksScreen(articles: List<Article>, onArticleClick: (Article) -> Unit, onBookmarkClick: (Article) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SavedSort.RECENT) }
    val colors = MaterialTheme.colorScheme
    val visible = filterAndSortBookmarks(articles, query, sort)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(stringResource(R.string.bookmarks_title), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.saved_description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text(stringResource(R.string.search_saved)) }, singleLine = true)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(SavedSort.entries, key = { it.name }) { option ->
                    FilterChip(selected = sort == option, onClick = { sort = option }, label = { Text(stringResource(when (option) {
                        SavedSort.RECENT -> R.string.sort_recent
                        SavedSort.OLDEST -> R.string.sort_oldest
                        SavedSort.TITLE -> R.string.sort_title
                    })) })
                }
            }
        }
        if (articles.isEmpty()) item { EmptyState(stringResource(R.string.empty_saved_title), stringResource(R.string.empty_saved_body)) }
        else if (visible.isEmpty()) item { EmptyState(stringResource(R.string.saved_search_empty), stringResource(R.string.saved_search_empty_body)) }
        items(visible, key = { it.id }) { article -> ArticleCard(article, true, { onArticleClick(article) }, { onBookmarkClick(article) }) }
    }
}

@Composable
private fun FeedFooter(state: FeedUiState, onLoadMore: () -> Unit, onRetry: () -> Unit) {
    when {
        state.pageError != null -> Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(state.pageError, color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onRetry) { Text(stringResource(R.string.retry)) } }
        state.isLoadingMore -> Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp)); Text(stringResource(R.string.loading_more), modifier = Modifier.padding(start = 10.dp)) }
        state.articles.isNotEmpty() && !state.canLoadMore -> Text(stringResource(R.string.end_of_results), modifier = Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ArticleCard(article: Article, isBookmarked: Boolean, onClick: () -> Unit, onBookmarkClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.25f
            if (compact) {
                Column(Modifier.padding(12.dp)) {
                    ArticleImage(article.imageUrl, article.title, Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(16.dp)))
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                        ArticleMetadata(article, Modifier.weight(1f))
                        BookmarkButton(isBookmarked, onBookmarkClick)
                    }
                }
            } else {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArticleImage(article.imageUrl, article.title, Modifier.size(width = 96.dp, height = 120.dp).clip(RoundedCornerShape(16.dp)))
                    ArticleMetadata(article, Modifier.weight(1f).padding(start = 14.dp, end = 4.dp))
                    BookmarkButton(isBookmarked, onBookmarkClick)
                }
            }
        }
    }
}

@Composable
private fun ArticleMetadata(article: Article, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Text(article.publisher.uppercase(), color = colors.secondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(5.dp))
        Text(article.title, color = colors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (article.summary.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(article.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(7.dp))
        Text(formatPublishedAt(article.publishedAt), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

@Composable
private fun BookmarkButton(isBookmarked: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            stringResource(if (isBookmarked) R.string.remove_bookmark else R.string.save_article),
            tint = if (isBookmarked) colors.primary else colors.onSurfaceVariant)
    }
}

@Composable
private fun LoadingState(message: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = colors.primary)
        Spacer(Modifier.height(14.dp))
        Text(message, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyIcon()
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.signal_lost), color = colors.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(15.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyIcon()
        Spacer(Modifier.height(18.dp))
        Text(title, color = colors.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text(body, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyIcon() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = colors.surfaceVariant, modifier = Modifier.size(72.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Public, null, tint = colors.tertiary, modifier = Modifier.size(34.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailsScreen(
    details: ArticleDetailsUiState,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    readingSize: ReadingSize,
    speechSpeed: SpeechSpeed,
    onSpeechSpeedChange: (SpeechSpeed) -> Unit
) {
    val article = details.article
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var speechState by remember(article.id) { mutableStateOf(SpeechState.Initializing) }
    val speechController = remember(article.id) { ArticleSpeechController(context) { speechState = it } }
    DisposableEffect(speechController, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) speechController.stop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); speechController.shutdown() }
    }
    val colors = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shareError = stringResource(R.string.article_share_handler_missing)
    val openError = stringResource(R.string.article_open_handler_missing)
    val missingUrl = stringResource(R.string.article_original_missing)
    val invalidUrl = stringResource(R.string.article_link_invalid)
    val shareChooserTitle = stringResource(R.string.share_chooser_title)
    val shareText = stringResource(R.string.share_article_text, article.title, article.url)
    val showMessage: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transmission_title), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.4.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back), tint = colors.onSurface) } },
                actions = {
                    IconButton(onClick = onBookmarkClick) {
                        Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            stringResource(if (isBookmarked) R.string.remove_bookmark else R.string.save_article), tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ArticleImage(article.imageUrl, article.title, Modifier.fillMaxWidth().height(255.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp)) {
                Text(stringResource(article.category.labelResource()).uppercase(), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(article.title, color = colors.onBackground, style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize * readingSize.multiplier, lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * readingSize.multiplier), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.article_publisher_date, article.publisher, formatPublishedAt(article.publishedAt)), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                if (article.summary.isNotBlank()) {
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.api_summary_label), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(article.summary, color = colors.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * readingSize.multiplier, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * readingSize.multiplier))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                        Button(onClick = { speechController.play(article.summary, speechSpeed.rate) }, enabled = speechState != SpeechState.Speaking) { Text(stringResource(R.string.read_aloud_play)) }
                        OutlinedButton(onClick = speechController::stop) { Text(stringResource(R.string.read_aloud_stop)) }
                    }
                    Text(stringResource(when (speechState) { SpeechState.Initializing -> R.string.speech_initializing; SpeechState.Speaking -> R.string.speech_speaking; SpeechState.Error -> R.string.speech_error; SpeechState.LanguageUnavailable -> R.string.speech_language_unavailable; SpeechState.NoSummary -> R.string.speech_no_summary; SpeechState.Ready -> R.string.speech_ready }), modifier = Modifier.padding(top = 6.dp), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { items(SpeechSpeed.entries, key = { it.name }) { speed -> FilterChip(selected = speed == speechSpeed, onClick = { onSpeechSpeedChange(speed) }, label = { Text(stringResource(when (speed) { SpeechSpeed.SLOW -> R.string.speed_slow; SpeechSpeed.NORMAL -> R.string.speed_normal; SpeechSpeed.FAST -> R.string.speed_fast })) }) } }
                }
                if (details.isLoading) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
                        Text(stringResource(R.string.updating_article), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(26.dp))
                Button(
                    onClick = {
                        val uri = validatedArticleUrl(article.url)
                        if (uri == null) showMessage(if (article.url.isBlank()) missingUrl else invalidUrl)
                        else try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toASCIIString())))
                        } catch (_: ActivityNotFoundException) {
                            showMessage(openError)
                        } catch (_: SecurityException) {
                            showMessage(openError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.article_open_original)) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val uri = validatedArticleUrl(article.url)
                        if (uri == null) showMessage(if (article.url.isBlank()) missingUrl else invalidUrl)
                        else try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareChooserTitle))
                        } catch (_: ActivityNotFoundException) {
                            showMessage(shareError)
                        } catch (_: SecurityException) {
                            showMessage(shareError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.article_share), modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.HorizontalDivider(color = colors.outlineVariant)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = colors.surfaceVariant, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Public, null, tint = colors.secondary) } }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.desk_name), color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.desk_tagline), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(themeMode: ThemeMode, readingSize: ReadingSize, speechSpeed: SpeechSpeed, interests: Set<NewsInterest>, onThemeModeChange: (ThemeMode) -> Unit, onReadingSizeChange: (ReadingSize) -> Unit, onSpeechSpeedChange: (SpeechSpeed) -> Unit, onInterestsChange: (Set<NewsInterest>) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.settings_description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(26.dp)); Text(stringResource(R.string.settings_theme_heading), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp)
        ThemeOption(ThemeMode.SYSTEM, themeMode, R.string.theme_system, R.string.theme_system_description, onThemeModeChange); ThemeOption(ThemeMode.LIGHT, themeMode, R.string.theme_light, R.string.theme_light_description, onThemeModeChange); ThemeOption(ThemeMode.DARK, themeMode, R.string.theme_dark, R.string.theme_dark_description, onThemeModeChange)
        Spacer(Modifier.height(22.dp)); Text(stringResource(R.string.reading_size), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(ReadingSize.entries, key = { it.name }) { size -> FilterChip(selected = size == readingSize, onClick = { onReadingSizeChange(size) }, label = { Text(stringResource(when(size) { ReadingSize.SMALL -> R.string.size_small; ReadingSize.MEDIUM -> R.string.size_medium; ReadingSize.LARGE -> R.string.size_large })) }) } }
        Spacer(Modifier.height(22.dp)); Text(stringResource(R.string.settings_speech_heading), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SpeechSpeed.entries, key = { it.name }) { speed ->
                FilterChip(selected = speed == speechSpeed, onClick = { onSpeechSpeedChange(speed) }, label = { Text(stringResource(when (speed) {
                    SpeechSpeed.SLOW -> R.string.speed_slow
                    SpeechSpeed.NORMAL -> R.string.speed_normal
                    SpeechSpeed.FAST -> R.string.speed_fast
                })) })
            }
        }
        Spacer(Modifier.height(22.dp)); Text(stringResource(R.string.interests_heading), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp)
        Text(stringResource(R.string.interests_description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 8.dp)) { NewsInterest.entries.forEach { interest -> FilterChip(selected = interest in interests, onClick = { onInterestsChange(if (interest in interests) interests - interest else interests + interest) }, label = { Text(interest.name.lowercase().replaceFirstChar(Char::uppercase)) }) } }
        Spacer(Modifier.height(24.dp)); Text(stringResource(R.string.settings_about_heading), color = colors.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.4.sp); Spacer(Modifier.height(10.dp)); Text(stringResource(R.string.settings_about_body), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp)); Text(stringResource(R.string.settings_footer), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: ThemeMode, title: Int, description: Int, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onSelect(mode) }).padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(stringResource(title), color = colors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        RadioButton(selected = mode == selected, onClick = null)
    }
}

private fun NewsCategory.labelResource(): Int = when (this) {
    NewsCategory.NEWS -> R.string.topic_all
    NewsCategory.LAUNCHES -> R.string.topic_launches
    NewsCategory.NASA -> R.string.topic_nasa
    NewsCategory.SPACEX -> R.string.topic_spacex
    NewsCategory.ASTRONOMY -> R.string.topic_astronomy
}

@Composable
private fun formatPublishedAt(value: String): String {
    if (value.isBlank()) return stringResource(R.string.publication_date_unavailable)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    return runCatching {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(value))
    }.getOrDefault(value)
}
