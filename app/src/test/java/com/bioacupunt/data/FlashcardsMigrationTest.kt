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
 * Guarda a migração 18 → 19 (Educação/Flashcards). Diferente de outras migrações do
 * app, esta não depende de nenhuma tabela pré-existente — `flashcards` e
 * `flashcard_progress` não têm FK. O risco real é o índice único
 * (tenantId, cardKey): sem ele, duas revisões do mesmo card no mesmo tenant
 * poderiam duplicar a linha de progresso em vez de atualizá-la.
 */
@RunWith(RobolectricTestRunner::class)
class FlashcardsMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath(DB_NAME).takeIf { it.exists() }?.delete()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                    // v18 não tem relação com flashcards — nada a criar de partida.
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() = helper.close()

    @Test
    fun `both tables exist after the migration`() {
        runMigration18to19()

        val tables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }
        assertTrue(tables.contains("flashcards"))
        assertTrue(tables.contains("flashcard_progress"))
    }

    @Test
    fun `a card can be inserted after the migration`() {
        runMigration18to19()

        db.execSQL(
            "INSERT INTO flashcards (tenantId, front, back, category, sourceArticleId, sourceSection, createdAt, updatedAt) " +
                "VALUES (1, 'Frente', 'Verso', 'Teoria', '', '', '2026-07-25T00:00:00Z', '2026-07-25T00:00:00Z')"
        )

        db.query("SELECT COUNT(*) FROM flashcards").use { cursor ->
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun `the unique index rejects a duplicate tenantId plus cardKey`() {
        runMigration18to19()
        insertProgress(tenantId = 1, cardKey = "builtin_de_qi")

        val threw = runCatching { insertProgress(tenantId = 1, cardKey = "builtin_de_qi") }.isFailure

        assertTrue("duas revisões do mesmo card no mesmo tenant não podem duplicar a linha", threw)
    }

    @Test
    fun `the same cardKey is allowed for a different tenant`() {
        runMigration18to19()
        insertProgress(tenantId = 1, cardKey = "builtin_de_qi")

        val threw = runCatching { insertProgress(tenantId = 2, cardKey = "builtin_de_qi") }.isFailure

        assertTrue("o índice único é composto (tenantId, cardKey) — outro tenant não pode ficar bloqueado", !threw)
        db.query("SELECT COUNT(*) FROM flashcard_progress WHERE cardKey = 'builtin_de_qi'").use { cursor ->
            cursor.moveToNext()
            assertEquals(2, cursor.getInt(0))
        }
    }

    private fun insertProgress(tenantId: Long, cardKey: String) {
        db.execSQL(
            "INSERT INTO flashcard_progress (tenantId, cardKey, box, dueAtEpochMs, lastReviewedAtEpochMs, totalReviews, totalLapses) " +
                "VALUES (?, ?, 0, 0, 0, 0, 0)",
            arrayOf<Any>(tenantId, cardKey),
        )
    }

    private fun runMigration18to19() {
        val migration = DatabaseModule.migrations()
            .first { it.startVersion == 18 && it.endVersion == 19 }
        db.beginTransaction()
        try {
            migration.migrate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        const val DB_NAME = "migration_v19_test.db"
    }
}
