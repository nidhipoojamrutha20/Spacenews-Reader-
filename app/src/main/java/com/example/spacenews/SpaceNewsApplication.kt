package com.example.spacenews

import android.app.Application
import androidx.room.Room
import com.example.spacenews.data.local.BookmarkRepository
import com.example.spacenews.data.local.SpaceNewsDatabase
import com.example.spacenews.data.preferences.ThemePreferences

class SpaceNewsApplication : Application() {
    val database: SpaceNewsDatabase by lazy {
        Room.databaseBuilder(this, SpaceNewsDatabase::class.java, "space-news.db")
            .addMigrations(SpaceNewsDatabase.MIGRATION_1_2)
            .build()
    }
    val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(database.bookmarkDao()) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(applicationContext) }
}
