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
 * Guards migration 13 → 14, which gives the syncable tables their sync identity.
 *
 * The hazard here is the unique index on `clientId`. Every row that existed
 * before this migration carries the column default `''`. Creating a unique index
 * over a column where every row is the empty string fails outright — and if it
 * somehow did not, every pre-existing patient would share one identity, so the
 * server would read the second as an edit of the first and one of them would
 * disappear. The backfill has to run, and it has to produce distinct values.
 */
@RunWith(RobolectricTestRunner::class)
class SyncIdentityMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath(DB_NAME).takeIf { it.exists() }?.delete()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createV13Schema(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() = helper.close()

    @Test
    fun `existing rows each get a distinct client id`() {
        insertPatient(1, "Ana Lima")
        insertPatient(2, "Carlos Souza")
        insertPatient(3, "Maria Santos")

        runMigration13to14()

        val ids = mutableSetOf<String>()
        db.query("SELECT clientId FROM crm_patients").use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getString(0))
        }
        assertEquals("every pre-existing patient needs its own identity", 3, ids.size)
        assertTrue("none may be left blank", ids.none { it.isBlank() })
    }

    @Test
    fun `a second patient can still be created after the migration`() {
        // The regression this whole test file exists for: if the backfill or the
        // unique index were wrong, creating the second patient would fail with a
        // constraint violation and the doctor would be back to being unable to
        // save anything.
        insertPatient(1, "Ana Lima")
        runMigration13to14()

        db.execSQL(
            "INSERT INTO crm_patients (tenantId, name, phone, email, birthDate, stage, " +
                "totalSessions, totalRevenueBrl, lastVisit, nextAppointment, tags, notes, " +
                "referralSource, npsScore, healthInsurance, mainComplaint, createdAt, updatedAt, " +
                "pendingSync, deleted, lastModified, clientId, serverId, baseRev) " +
                "VALUES (1, 'Nova Paciente', '', '', '', 'ACTIVE', 0, 0.0, '', '', '', '', '', " +
                "NULL, '', '', '', '', 1, 0, '', 'uuid-nova', NULL, 0)"
        )

        db.query("SELECT COUNT(*) FROM crm_patients").use { cursor ->
            cursor.moveToNext()
            assertEquals(2, cursor.getInt(0))
        }
    }

    @Test
    fun `two rows cannot share a client id`() {
        insertPatient(1, "Ana Lima")
        runMigration13to14()

        val existing = db.query("SELECT clientId FROM crm_patients LIMIT 1").use {
            it.moveToNext(); it.getString(0)
        }

        val threw = runCatching {
            db.execSQL(
                "INSERT INTO crm_patients (tenantId, name, phone, email, birthDate, stage, " +
                    "totalSessions, totalRevenueBrl, lastVisit, nextAppointment, tags, notes, " +
                    "referralSource, npsScore, healthInsurance, mainComplaint, createdAt, updatedAt, " +
                    "pendingSync, deleted, lastModified, clientId, serverId, baseRev) " +
                    "VALUES (1, 'Impostora', '', '', '', 'ACTIVE', 0, 0.0, '', '', '', '', '', " +
                    "NULL, '', '', '', '', 1, 0, '', ?, NULL, 0)",
                arrayOf<Any>(existing),
            )
        }.isFailure
        assertTrue("duplicate client ids would merge two patients on the server", threw)
    }

    @Test
    fun `the sync cursor starts at zero so a fresh device pulls everything`() {
        runMigration13to14()
        db.query("SELECT lastPulledRev FROM sync_state WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals(0L, cursor.getLong(0))
        }
    }

    @Test
    fun `the conflicts table starts empty and accepts both versions of a record`() {
        runMigration13to14()
        db.execSQL(
            "INSERT INTO sync_conflicts (entityType, clientId, serverId, serverRev, " +
                "localPayloadJson, serverPayloadJson, detectedAt) " +
                "VALUES ('patient', 'c1', 42, 9, '{\"name\":\"celular\"}', '{\"name\":\"tablet\"}', '2026-07-18')"
        )
        db.query("SELECT localPayloadJson, serverPayloadJson, resolvedAt FROM sync_conflicts").use { c ->
            assertTrue(c.moveToNext())
            assertTrue(c.getString(0).contains("celular"))
            assertTrue(c.getString(1).contains("tablet"))
            assertTrue("a fresh conflict is unresolved", c.isNull(2))
        }
    }

    private fun runMigration13to14() {
        val migration = DatabaseModule.migrations()
            .first { it.startVersion == 13 && it.endVersion == 14 }
        db.beginTransaction()
        try {
            migration.migrate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertPatient(id: Long, name: String) {
        db.execSQL(
            "INSERT INTO crm_patients (id, tenantId, name, phone, email, birthDate, stage, " +
                "totalSessions, totalRevenueBrl, lastVisit, nextAppointment, tags, notes, " +
                "referralSource, npsScore, healthInsurance, mainComplaint, createdAt, updatedAt, " +
                "pendingSync, deleted, lastModified) " +
                "VALUES (?, 1, ?, '', '', '', 'ACTIVE', 0, 0.0, '', '', '', '', '', NULL, '', '', '', '', 0, 0, '')",
            arrayOf<Any>(id, name),
        )
    }

    /** The three syncable tables as they stood at v13, before sync identity. */
    private fun createV13Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `crm_patients` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`tenantId` INTEGER NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL DEFAULT '', " +
                "`email` TEXT NOT NULL DEFAULT '', `birthDate` TEXT NOT NULL DEFAULT '', " +
                "`stage` TEXT NOT NULL DEFAULT 'FIRST_CONTACT', `totalSessions` INTEGER NOT NULL DEFAULT 0, " +
                "`totalRevenueBrl` REAL NOT NULL DEFAULT 0.0, `lastVisit` TEXT NOT NULL DEFAULT '', " +
                "`nextAppointment` TEXT NOT NULL DEFAULT '', `tags` TEXT NOT NULL DEFAULT '', " +
                "`notes` TEXT NOT NULL DEFAULT '', `referralSource` TEXT NOT NULL DEFAULT '', " +
                "`npsScore` INTEGER, `healthInsurance` TEXT NOT NULL DEFAULT '', " +
                "`mainComplaint` TEXT NOT NULL DEFAULT '', `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT '', `pendingSync` INTEGER NOT NULL DEFAULT 0, " +
                "`deleted` INTEGER NOT NULL DEFAULT 0, `lastModified` TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE `appointments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`tenantId` INTEGER NOT NULL, `patientId` INTEGER NOT NULL, `patientName` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, `time` TEXT NOT NULL, `durationMin` INTEGER NOT NULL DEFAULT 60, " +
                "`type` TEXT NOT NULL DEFAULT 'ACUPUNCTURE', `status` TEXT NOT NULL DEFAULT 'SCHEDULED', " +
                "`notes` TEXT NOT NULL DEFAULT '', `sessionNumber` INTEGER NOT NULL DEFAULT 1, " +
                "`reminderMinutesBefore` INTEGER NOT NULL DEFAULT 30, `valueBrl` REAL NOT NULL DEFAULT 0.0, " +
                "`paid` INTEGER NOT NULL DEFAULT 0, `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT '', `pendingSync` INTEGER NOT NULL DEFAULT 0, " +
                "`deleted` INTEGER NOT NULL DEFAULT 0, `lastModified` TEXT NOT NULL DEFAULT '')"
        )
        db.execSQL(
            "CREATE TABLE `transacoes` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER, `appointmentId` INTEGER, `amountBrl` REAL NOT NULL DEFAULT 0.0, " +
                "`date` TEXT NOT NULL DEFAULT '', `type` TEXT NOT NULL DEFAULT 'PAGAMENTO', " +
                "`method` TEXT NOT NULL DEFAULT 'PIX', `category` TEXT NOT NULL DEFAULT 'SESSÃO', " +
                "`status` TEXT NOT NULL DEFAULT 'PAGO', `notes` TEXT NOT NULL DEFAULT '', " +
                "`pendingSync` INTEGER NOT NULL DEFAULT 0, `deleted` INTEGER NOT NULL DEFAULT 0, " +
                "`createdAt` TEXT NOT NULL DEFAULT '', `updatedAt` TEXT NOT NULL DEFAULT '', " +
                "`lastModified` TEXT NOT NULL DEFAULT '')"
        )
    }

    private companion object {
        const val DB_NAME = "migration_v14_test.db"
    }
}
