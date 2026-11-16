package com.example.spacenews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BookmarkEntity::class], version = 2, exportSchema = false)
abstract class SpaceNewsDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bookmarks ADD COLUMN savedAt INTEGER")
            }
        }
    }
}
