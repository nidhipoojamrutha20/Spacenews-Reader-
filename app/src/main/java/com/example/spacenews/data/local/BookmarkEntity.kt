package com.example.spacenews.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publisher: String,
    val publishedAt: String,
    val url: String,
    val category: String,
    val savedAt: Long? = null
)

fun Article.toBookmarkEntity(savedAt: Long? = this.savedAt) = BookmarkEntity(
    id = id, title = title, summary = summary, imageUrl = imageUrl, publisher = publisher,
    publishedAt = publishedAt, url = url, category = category.name, savedAt = savedAt
)

fun BookmarkEntity.toArticle() = Article(
    id = id, title = title, summary = summary, imageUrl = imageUrl, publisher = publisher,
    publishedAt = publishedAt, url = url,
    category = runCatching { NewsCategory.valueOf(category) }.getOrDefault(NewsCategory.NEWS),
    savedAt = savedAt
)
