package com.example.spacenews.data.local

import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkSearchTest {
    private val marsRover = article(1, "Mars Rover Finds Water")
    private val moonBase = article(2, "Moon  Base Plans")      // title has a double space
    private val starship = article(3, "Starship Launch Update")
    private val all = listOf(marsRover, moonBase, starship)

    private fun search(query: String) =
        filterAndSortBookmarks(all, query, SavedSort.OLDEST).map { it.id }

    @Test fun blankOrWhitespaceQueryReturnsAll() {
        assertEquals(listOf(1, 2, 3), search(""))
        assertEquals(listOf(1, 2, 3), search("   "))
        assertEquals(listOf(1, 2, 3), search("\t "))
    }

    @Test fun leadingAndTrailingSpacesAreIgnored() {
        assertEquals(listOf(1), search("  mars rover  "))
    }

    @Test fun searchIsCaseInsensitive() {
        assertEquals(listOf(1), search("MARS ROVER"))
        assertEquals(listOf(3), search("sTaRsHiP"))
    }

    @Test fun repeatedSpacesInsideQueryStillMatch() {
        assertEquals(listOf(1), search("mars    rover"))
        assertEquals(listOf(1), search("mars\trover"))
    }

    @Test fun extraSpacesInsideTitleStillMatch() {
        assertEquals(listOf(2), search("moon base"))
    }

    @Test fun nonMatchingQueryReturnsEmpty() {
        assertEquals(emptyList<Int>(), search("jupiter"))
    }

    @Test fun normalizeCollapsesWhitespace() {
        assertEquals("a b c", normalizeForSearch("  a \t b   c "))
    }

    private fun article(id: Int, title: String) = Article(
        id, title, "summary", null, "Publisher", "2026-01-0${id}T00:00:00Z",
        "https://example.test/$id", NewsCategory.NEWS, savedAt = id.toLong()
    )
}
