package com.example.spacenews.data.local

import com.example.spacenews.data.model.Article

enum class SavedSort { RECENT, OLDEST, TITLE }

/** Legacy rows with no save timestamp remain untimestamped and sort after known dates. */
fun filterAndSortBookmarks(articles: List<Article>, query: String, sort: SavedSort): List<Article> =
    articles.filter { it.title.contains(query.trim(), ignoreCase = true) }.sortedWith(when (sort) {
        SavedSort.RECENT -> compareByDescending<Article> { it.savedAt ?: Long.MIN_VALUE }.thenByDescending { it.id }
        SavedSort.OLDEST -> compareBy<Article> { it.savedAt ?: Long.MAX_VALUE }.thenBy { it.id }
        SavedSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    })
