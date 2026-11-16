package com.example.spacenews.data.local

import com.example.spacenews.data.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(
    private val dao: BookmarkDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val bookmarks: Flow<List<Article>> = dao.observeAll().map { rows -> rows.map(BookmarkEntity::toArticle) }
    val bookmarkIds: Flow<Set<Int>> = dao.observeIds().map { it.toSet() }

    suspend fun isSaved(articleId: Int): Boolean = dao.isSaved(articleId)

    /** Existing save times, including legacy null values, are preserved on replacement. */
    suspend fun save(article: Article) {
        val existing = dao.getById(article.id)
        val savedAt = if (existing != null) existing.savedAt else article.savedAt ?: clock()
        dao.insert(article.toBookmarkEntity(savedAt))
    }

    suspend fun remove(articleId: Int) = dao.delete(articleId)
}
