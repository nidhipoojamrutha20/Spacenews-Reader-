package com.example.spacenews.data.model

enum class NewsCategory(val searchTerm: String?) {
    NEWS(null),
    LAUNCHES("launch"),
    NASA("NASA"),
    SPACEX("SpaceX"),
    ASTRONOMY("astronomy")
}

enum class NewsInterest(val searchTerm: String) {
    ISRO("ISRO"),
    NASA("NASA"),
    SPACEX("SpaceX"),
    MARS("Mars"),
    MOON("Moon"),
    SATELLITES("satellite")
}

data class Article(
    val id: Int,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publisher: String,
    val publishedAt: String,
    val url: String,
    val category: NewsCategory = NewsCategory.NEWS,
    /** Null for API articles and for bookmarks created before savedAt was introduced. */
    val savedAt: Long? = null
) {
    val bookmarkKey: String get() = id.toString()
}
