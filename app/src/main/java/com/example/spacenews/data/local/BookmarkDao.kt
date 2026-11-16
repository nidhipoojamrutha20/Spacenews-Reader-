package com.example.spacenews.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC, id DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT id FROM bookmarks")
    fun observeIds(): Flow<List<Int>>

    @Query("SELECT * FROM bookmarks WHERE id = :articleId LIMIT 1")
    suspend fun getById(articleId: Int): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :articleId)")
    suspend fun isSaved(articleId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :articleId")
    suspend fun delete(articleId: Int)
}
