package com.example.spacenews

import com.example.spacenews.data.util.validatedArticleUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleLinkTest {
    @Test fun acceptsHttpAndHttpsUrlsWithHosts() {
        assertEquals("https", validatedArticleUrl("https://example.org/story")?.scheme)
        assertEquals("http", validatedArticleUrl("http://example.org/story")?.scheme)
    }

    @Test fun rejectsNonWebMalformedAndHostlessUrls() {
        assertNull(validatedArticleUrl("javascript:alert(1)"))
        assertNull(validatedArticleUrl("https:///story"))
        assertNull(validatedArticleUrl("not a url"))
    }
}
