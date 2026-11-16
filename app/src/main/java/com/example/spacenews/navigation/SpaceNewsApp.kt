package com.example.spacenews.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.spacenews.SpaceNewsApplication
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.spacenews.R
import com.example.spacenews.screens.ArticleDetailsScreen
import com.example.spacenews.screens.BookmarksScreen
import com.example.spacenews.screens.CategoriesScreen
import com.example.spacenews.screens.HomeScreen
import com.example.spacenews.screens.ForYouScreen
import com.example.spacenews.screens.SettingsScreen
import com.example.spacenews.data.model.NewsCategory
import com.example.spacenews.ui.viewmodel.NewsViewModel
import com.example.spacenews.ui.viewmodel.SettingsViewModel

private data class Tab(val route: String, val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", R.string.nav_home, Icons.Filled.Home),
    Tab("categories", R.string.nav_categories, Icons.Filled.Explore),
    Tab("for_you", R.string.nav_for_you, Icons.Filled.AutoAwesome),
    Tab("bookmarks", R.string.nav_bookmarks, Icons.Filled.BookmarkBorder),
    Tab("settings", R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun SpaceNewsApp() {
    val application = LocalContext.current.applicationContext as SpaceNewsApplication
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory(application))
    val settingsViewModel: SettingsViewModel = viewModel()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val readingSize by settingsViewModel.readingSize.collectAsStateWithLifecycle()
    val speechSpeed by settingsViewModel.speechSpeed.collectAsStateWithLifecycle()
    val interests by newsViewModel.interests.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val homeState by newsViewModel.homeState.collectAsStateWithLifecycle()
    val exploreState by newsViewModel.exploreState.collectAsStateWithLifecycle()
    val forYouState by newsViewModel.forYouState.collectAsStateWithLifecycle()
    val query by newsViewModel.query.collectAsStateWithLifecycle()
    val selectedCategory by newsViewModel.selectedCategory.collectAsStateWithLifecycle()
    val bookmarks by newsViewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val savedArticles by newsViewModel.bookmarks.collectAsStateWithLifecycle()
    val articleDetails by newsViewModel.articleDetails.collectAsStateWithLifecycle()
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                        label = { Text(stringResource(tab.label)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(homeState, bookmarks,
                    onRefresh = newsViewModel::refreshHome, onLoadMore = newsViewModel::loadMoreHome, onRetryPage = newsViewModel::retryHomePage,
                    onArticleClick = { newsViewModel.loadArticleDetails(it); navController.navigate("article") },
                    onBookmarkClick = newsViewModel::toggleBookmark,
                    onExploreClick = { navController.navigate("categories") })
            }
            composable("categories") {
                LaunchedEffect(Unit) {
                    if (exploreState.articles.isEmpty() && exploreState.error == null && !exploreState.isLoading) newsViewModel.selectCategory(selectedCategory)
                }
                CategoriesScreen(exploreState, selectedCategory, query, bookmarks,
                    onQueryChange = newsViewModel::setQuery, onSelectCategory = newsViewModel::selectCategory,
                    onRetry = { newsViewModel.refreshExplore() }, onRefresh = newsViewModel::refreshExplore, onLoadMore = newsViewModel::loadMoreExplore, onRetryPage = newsViewModel::loadMoreExplore,
                    onArticleClick = { newsViewModel.loadArticleDetails(it); navController.navigate("article") },
                    onBookmarkClick = newsViewModel::toggleBookmark)
            }
            composable("for_you") {
                androidx.compose.runtime.LaunchedEffect(Unit) { if (forYouState.articles.isEmpty() && forYouState.error == null) newsViewModel.refreshForYou() }
                ForYouScreen(forYouState, interests, bookmarks, newsViewModel::refreshForYou, newsViewModel::loadMoreForYou, newsViewModel::refreshForYou,
                    onArticleClick = { newsViewModel.loadArticleDetails(it); navController.navigate("article") },
                    onBookmarkClick = newsViewModel::toggleBookmark,
                    onEditInterests = { navController.navigate("settings") })
            }
            composable("bookmarks") {
                BookmarksScreen(savedArticles,
                    onArticleClick = { newsViewModel.loadArticleDetails(it); navController.navigate("article") },
                    onBookmarkClick = newsViewModel::toggleBookmark)
            }
            composable("settings") { SettingsScreen(themeMode, readingSize, speechSpeed, interests, settingsViewModel::setThemeMode, settingsViewModel::setReadingSize, settingsViewModel::setSpeechSpeed, settingsViewModel::setInterests) }
            composable("article") {
                val details = articleDetails
                if (details != null) ArticleDetailsScreen(details, details.article.id in bookmarks,
                    onBack = { newsViewModel.clearArticleDetails(); navController.popBackStack() },
                    onBookmarkClick = { newsViewModel.toggleBookmark(details.article) }, readingSize = readingSize, speechSpeed = speechSpeed, onSpeechSpeedChange = settingsViewModel::setSpeechSpeed)
            }
        }
    }
}
