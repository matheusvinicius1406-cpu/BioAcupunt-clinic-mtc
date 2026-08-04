package com.bioacupunt.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.bioacupunt.di.DatabaseModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guarda a migração 23 → 24 (tradutor automático da Biblioteca). `article_translations` não
 * depende de nenhuma tabela pré-existente — sem FK, mesmo raciocínio de flashcards/medicamentos
 * (reaprovar/reimportar um artigo nunca pode travar numa FK). O risco real é a chave composta
 * (articleId, targetLanguage): sem ela, re-traduzir o mesmo artigo para o mesmo idioma
 * duplicaria a linha em vez de substituí-la.
 */
@RunWith(RobolectricTestRunner::class)
class ArticleTranslationMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath(DB_NAME).takeIf { it.exists() }?.delete()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                    // v23 não tem relação com article_translations — nada a criar de partida.
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build(),
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() = helper.close()

    @Test
    fun `table exists after the migration`() {
        runMigration23to24()

        val tables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }
        assertTrue(tables.contains("article_translations"))
    }

    @Test
    fun `a translation row can be inserted and read back`() {
        runMigration23to24()
        insertRow(articleId = "art-1", language = "en", status = "COMPLETED")

        db.query("SELECT status FROM article_translations WHERE articleId = 'art-1' AND targetLanguage = 'en'").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals("COMPLETED", cursor.getString(0))
        }
    }

    @Test
    fun `the composite primary key rejects a duplicate articleId plus targetLanguage`() {
        runMigration23to24()
        insertRow(articleId = "art-1", language = "en", status = "PENDING")

        val threw = runCatching { insertRow(articleId = "art-1", language = "en", status = "PENDING") }.isFailure

        assertTrue("re-traduzir o mesmo artigo/idioma não pode duplicar a linha", threw)
    }

    @Test
    fun `the same article in a different target language is a separate row`() {
        runMigration23to24()
        insertRow(articleId = "art-1", language = "en", status = "COMPLETED")

        val threw = runCatching { insertRow(articleId = "art-1", language = "es", status = "PENDING") }.isFailure

        assertTrue("idiomas diferentes do mesmo artigo não podem colidir", !threw)
        db.query("SELECT COUNT(*) FROM article_translations WHERE articleId = 'art-1'").use { cursor ->
            cursor.moveToNext()
            assertEquals(2, cursor.getInt(0))
        }
    }

    private fun insertRow(articleId: String, language: String, status: String) {
        db.execSQL(
            "INSERT INTO article_translations (articleId, targetLanguage, status, title, summary, content, tagsCsv, errorMessage, updatedAt) " +
                "VALUES (?, ?, ?, '', '', '', '', '', 0)",
            arrayOf<Any>(articleId, language, status),
        )
    }

    private fun runMigration23to24() {
        val migration = DatabaseModule.migrations()
            .first { it.startVersion == 23 && it.endVersion == 24 }
        db.beginTransaction()
        try {
            migration.migrate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        const val DB_NAME = "migration_v24_test.db"
    }
}
