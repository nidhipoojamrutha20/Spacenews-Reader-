package com.example.spacenews.data.util

import java.net.URI

/** Accept only absolute web links with a host; rejects malformed and non-web intent targets. */
fun validatedArticleUrl(value: String): URI? = runCatching {
    URI(value.trim()).takeIf {
        it.isAbsolute && it.userInfo == null && !it.host.isNullOrBlank() &&
            (it.scheme.equals("http", ignoreCase = true) || it.scheme.equals("https", ignoreCase = true))
    }
}.getOrNull()
