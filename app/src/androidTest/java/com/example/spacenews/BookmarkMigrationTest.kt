package com.example.spacenews

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spacenews.data.local.BookmarkRepository
import com.example.spacenews.data.local.SpaceNewsDatabase
import com.example.spacenews.data.local.toArticle
import com.example.spacenews.data.model.Article
import com.example.spacenews.data.model.NewsCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test fun savedArticlesSurviveUpgradeAndRemainReadableThroughRoom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "bookmark-upgrade-test-${System.nanoTime()}.db"

        // 1. Build a real version-1 database file containing two saved articles.
        val v1Config = SupportSQLiteOpenHelper.Configuration.builder(context).name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE bookmarks (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, imageUrl TEXT, publisher TEXT NOT NULL, publishedAt TEXT NOT NULL, url TEXT NOT NULL, category TEXT NOT NULL)")
                    db.execSQL("INSERT INTO bookmarks VALUES (11,'Artemis II crew named','NASA announces the crew.','https://img.test/11.jpg','NASA','2026-02-01T10:00:00Z','https://example.test/11','NASA')")
                    db.execSQL("INSERT INTO bookmarks VALUES (12,'Starship flight test','SpaceX flies again.',NULL,'SpaceX','2026-03-05T08:30:00Z','https://example.test/12','SPACEX')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        FrameworkSQLiteOpenHelperFactory().create(v1Config).apply { writableDatabase; close() }

        // 2. Open the same file with Room as the app does: runs MIGRATION_1_2 and Room's schema validation.
        val db = Room.databaseBuilder(context, SpaceNewsDatabase::class.java, dbName)
            .addMigrations(SpaceNewsDatabase.MIGRATION_1_2)
            .build()
        try {
            runBlocking {
                val dao = db.bookmarkDao()

                assertEquals(2, dao.observeAll().first().size)
                val artemis = dao.getById(11)
                assertNotNull(artemis)
                assertEquals("Artemis II crew named", artemis!!.title)
                assertEquals("NASA announces the crew.", artemis.summary)
                assertEquals("https://img.test/11.jpg", artemis.imageUrl)
                assertEquals("NASA", artemis.publisher)
                assertEquals("2026-02-01T10:00:00Z", artemis.publishedAt)
                assertEquals("https://example.test/11", artemis.url)
                assertNull(artemis.savedAt)

                val starship = dao.getById(12)!!
                assertNull(starship.imageUrl)
                assertEquals(NewsCategory.SPACEX, starship.toArticle().category)
                assertEquals(setOf(11, 12), dao.observeIds().first().toSet())

                // Re-saving a legacy bookmark keeps its null savedAt; a brand-new save gets a timestamp.
                val repository = BookmarkRepository(dao, clock = { 5_000L })
                repository.save(artemis.toArticle())
                assertNull(dao.getById(11)!!.savedAt)
                repository.save(Article(13, "New save", "s", null, "ESA", "2026-11-01T00:00:00Z", "https://example.test/13"))
                assertEquals(5_000L, dao.getById(13)!!.savedAt)

                // Removing still works on migrated data.
                repository.remove(12)
                assertEquals(false, dao.isSaved(12))
            }
        } finally {
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
