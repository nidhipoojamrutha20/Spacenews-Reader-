package com.example.spacenews.data.local

import com.example.spacenews.data.model.Article

enum class SavedSort { RECENT, OLDEST, TITLE }

private val WHITESPACE = Regex("\\s+")

/** Trims and collapses runs of whitespace (spaces, tabs) to a single space so searches match regardless of spacing. */
internal fun normalizeForSearch(text: String): String = text.trim().replace(WHITESPACE, " ")

/** Legacy rows with no save timestamp remain untimestamped and sort after known dates. */
fun filterAndSortBookmarks(articles: List<Article>, query: String, sort: SavedSort): List<Article> {
    val normalizedQuery = normalizeForSearch(query)
    return articles
        .filter { normalizeForSearch(it.title).contains(normalizedQuery, ignoreCase = true) }
        .sortedWith(when (sort) {
            SavedSort.RECENT -> compareByDescending<Article> { it.savedAt ?: Long.MIN_VALUE }.thenByDescending { it.id }
            SavedSort.OLDEST -> compareBy<Article> { it.savedAt ?: Long.MAX_VALUE }.thenBy { it.id }
            SavedSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        })
}
