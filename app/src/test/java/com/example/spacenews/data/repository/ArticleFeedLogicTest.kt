package com.example.spacenews.data.repository

import com.example.spacenews.data.local.SavedSort
import com.example.spacenews.data.local.filterAndSortBookmarks
import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleFeedLogicTest {
    @Test fun mergeDeduplicatesByIdAndSortsPublicationDate() {
        val old = article(1, "2026-01-01T00:00:00Z")
        val duplicate = article(1, "2026-01-02T00:00:00Z")
        val other = article(2, "2026-01-03T00:00:00Z")
        val result = mergeArticles(listOf(old), listOf(duplicate, other))
        assertEquals(listOf(2, 1), result.map { it.id })
        assertEquals("2026-01-01T00:00:00Z", result.last().publishedAt)
    }

    @Test fun bookmarkSearchAndSortUseSavedAtAndLeaveLegacyRowsUntimestamped() {
        val recently = article(1, "2026-05-01").copy(title = "Zeta", savedAt = 200)
        val oldest = article(2, "2026-04-01").copy(title = "Alpha", savedAt = 100)
        val legacy = article(3, "2026-03-01").copy(title = "Legacy", savedAt = null)
        val all = listOf(recently, oldest, legacy)
        assertEquals(listOf(1, 2, 3), filterAndSortBookmarks(all, "", SavedSort.RECENT).map { it.id })
        assertEquals(listOf(2, 1, 3), filterAndSortBookmarks(all, "", SavedSort.OLDEST).map { it.id })
        assertEquals(listOf(2, 3, 1), filterAndSortBookmarks(all, "", SavedSort.TITLE).map { it.id })
        assertEquals(listOf(3), filterAndSortBookmarks(all, "LEG", SavedSort.TITLE).map { it.id })
        assertEquals(null, legacy.savedAt)
    }

    private fun article(id: Int, published: String) = Article(id, "Article $id", "summary", null, "Publisher", published, "https://example.test/$id", NewsCategory.NEWS)
}
