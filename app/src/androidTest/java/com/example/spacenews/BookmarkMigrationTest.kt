package com.example.spacenews

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spacenews.data.local.SpaceNewsDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkMigrationTest {
    @Test fun migrationAddsNullableSavedAtWithoutChangingExistingBookmark() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "bookmark-migration-test-${System.nanoTime()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE bookmarks (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, imageUrl TEXT, publisher TEXT NOT NULL, publishedAt TEXT NOT NULL, url TEXT NOT NULL, category TEXT NOT NULL)")
                    db.execSQL("INSERT INTO bookmarks (id,title,summary,imageUrl,publisher,publishedAt,url,category) VALUES (7,'Saved title','saved summary',NULL,'ESA','2026-01-01','https://example.test/7','NEWS')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        try {
            val db = helper.writableDatabase
            SpaceNewsDatabase.MIGRATION_1_2.migrate(db)
            db.query("SELECT id,title,summary,savedAt FROM bookmarks WHERE id=7").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
                assertEquals("Saved title", cursor.getString(1))
                assertEquals("saved summary", cursor.getString(2))
                check(cursor.isNull(3))
            }
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }
}
