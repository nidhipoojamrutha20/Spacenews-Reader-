package com.example.spacenews.data.repository

import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import com.example.spacenews.data.remote.ApiPage
import com.example.spacenews.data.remote.SpaceflightNewsApi
import com.example.spacenews.data.remote.SpaceflightNewsDto

class NewsRepository(private val api: SpaceflightNewsApi, private val publisherFallback: String = "") {
    suspend fun fetchHome(): ArticlePage = api.getArticles(limit = PAGE_SIZE, ordering = "-published_at").toArticlePage(NewsCategory.NEWS)

    suspend fun searchArticles(query: String?, category: NewsCategory = NewsCategory.NEWS): ArticlePage =
        api.getArticles(limit = PAGE_SIZE, ordering = "-published_at", search = query?.trim()?.takeIf(String::isNotEmpty) ?: category.searchTerm)
            .toArticlePage(category)

    suspend fun fetchPage(nextUrl: String, category: NewsCategory = NewsCategory.NEWS): ArticlePage =
        api.getNextPage(nextUrl).toArticlePage(category)

    suspend fun fetchInterest(interestTerm: String): ArticlePage =
        api.getArticles(limit = INTEREST_PAGE_SIZE, ordering = "-published_at", search = interestTerm)
            .toArticlePage(NewsCategory.NEWS)

    suspend fun fetchArticle(id: Int): Article = api.getArticle(id).toArticle(NewsCategory.NEWS, publisherFallback)
        ?: throw IllegalStateException("The article response was incomplete.")

    private fun ApiPage<SpaceflightNewsDto>.toArticlePage(category: NewsCategory) = ArticlePage(
        articles = results.orEmpty().mapNotNull { it.toArticle(category, publisherFallback) },
        next = next
    )

    private companion object {
        const val PAGE_SIZE = 20
        const val INTEREST_PAGE_SIZE = 8
    }
}

data class ArticlePage(val articles: List<Article>, val next: String?)

internal fun SpaceflightNewsDto.toArticle(category: NewsCategory, publisherFallback: String = ""): Article? {
    val validId = id ?: return null
    val validTitle = title?.takeIf(String::isNotBlank) ?: return null
    return Article(
        id = validId,
        title = validTitle,
        summary = summary.orEmpty(),
        imageUrl = imageUrl?.takeIf(String::isNotBlank),
        publisher = newsSite?.takeIf(String::isNotBlank) ?: publisherFallback,
        publishedAt = publishedAt.orEmpty(),
        url = url.orEmpty(),
        category = category
    )
}

fun mergeArticles(existing: List<Article>, incoming: List<Article>): List<Article> =
    (existing + incoming).distinctBy(Article::id).sortedWith(
        compareByDescending<Article> { it.publishedAt }.thenByDescending(Article::id)
    )
