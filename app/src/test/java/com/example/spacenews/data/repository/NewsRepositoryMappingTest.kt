package com.example.spacenews.data.repository

import com.example.spacenews.data.model.NewsCategory
import com.example.spacenews.data.remote.ApiPage
import com.example.spacenews.data.remote.SpaceflightNewsDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NewsRepositoryMappingTest {
    @Test
    fun mapsLiveApiEnvelopeFieldsToArticle() {
        val response = """{
            "count": 1,
            "next": null,
            "previous": null,
            "results": [{
                "id": 40615,
                "title": "New contracts to modernise Galileo ground segment",
                "summary": "ESA and partners signed contracts for new ground stations.",
                "image_url": "https://example.org/galileo.jpg",
                "news_site": "ESA",
                "published_at": "2026-10-28T07:09:00Z",
                "url": "https://www.esa.int/story"
            }]
        }"""
        val type = object : TypeToken<ApiPage<SpaceflightNewsDto>>() {}.type
        val page: ApiPage<SpaceflightNewsDto> = Gson().fromJson(response, type)
        val article = page.results?.single()?.toArticle(NewsCategory.NEWS)

        assertNotNull(article)
        assertEquals(40615, article?.id)
        assertEquals("New contracts to modernise Galileo ground segment", article?.title)
        assertEquals("ESA and partners signed contracts for new ground stations.", article?.summary)
        assertEquals("https://example.org/galileo.jpg", article?.imageUrl)
        assertEquals("ESA", article?.publisher)
        assertEquals("2026-10-28T07:09:00Z", article?.publishedAt)
        assertEquals("https://www.esa.int/story", article?.url)
        assertEquals(NewsCategory.NEWS, article?.category)
    }
}
