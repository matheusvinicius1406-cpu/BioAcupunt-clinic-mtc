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
 * Guards migration 12 → 13, which repoints every clinical foreign key from the
 * legacy `patients` table to `crm_patients`.
 *
 * Why this test is not optional: before v13, the clinical tables referenced a
 * table the app never wrote to, so *every* attempt to save a chart, a vital
 * sign, an exam or a document was rejected by SQLite and surfaced to the doctor
 * as "Falha ao acessar dados locais". The migration that repairs that runs
 * exactly once, unattended, on a database holding real patient charts. It gets
 * one chance to be correct.
 *
 * The cases below are the ones that can hurt someone:
 *  - a chart must survive the rebuild with its content intact,
 *  - a chart must never be re-attached to a *different* patient,
 *  - a chart whose patient is missing must be rescued, never silently dropped.
 */
@RunWith(RobolectricTestRunner::class)
class PatientRegistryMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getDatabasePath(DB_NAME).takeIf { it.exists() }?.delete()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createV12Schema(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build()
        )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    @Test
    fun `chart survives the rebuild with its content intact`() {
        insertCrmPatient(id = 7, name = "Ana Lima")
        db.execSQL(
            "INSERT INTO mtc_assessments (id, patientId, chiefComplaint, flagsCsv, tongueJson) " +
                "VALUES (1, 7, 'Enxaqueca há 3 meses', 'PREGNANCY', '{\"color\":\"pale\"}')"
        )

        runMigration12to13()

        db.query("SELECT patientId, chiefComplaint, flagsCsv, tongueJson FROM mtc_assessments WHERE id = 1")
            .use { cursor ->
                assertTrue("the assessment must still exist", cursor.moveToNext())
                assertEquals(7L, cursor.getLong(0))
                assertEquals("Enxaqueca há 3 meses", cursor.getString(1))
                // The contraindication has to come through untouched — this is the
                // column the safety engine reads.
                assertEquals("PREGNANCY", cursor.getString(2))
                assertEquals("{\"color\":\"pale\"}", cursor.getString(3))
            }
    }

    @Test
    fun `chart is never reattached to a different patient`() {
        // Two unrelated people who happen to share id 3 across the two tables.
        // The CRM row is the one the doctor navigated by, so it is the right owner.
        insertCrmPatient(id = 3, name = "Carlos Souza")
        db.execSQL(
            "INSERT INTO patients (id, tenantId, name, document, createdAt, updatedAt, status, pendingSync) " +
                "VALUES (3, 1, 'Maria Santos', '789', '', '', 'ACTIVE', 0)"
        )
        db.execSQL("INSERT INTO vital_signs (id, patientId, label, value) VALUES (1, 3, 'PA', '120/80')")

        runMigration12to13()

        db.query("SELECT patientId FROM vital_signs WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals(3L, cursor.getLong(0))
        }
        db.query("SELECT name FROM crm_patients WHERE id = 3").use { cursor ->
            assertTrue(cursor.moveToNext())
            assertEquals("Carlos Souza", cursor.getString(0))
        }
    }

    @Test
    fun `orphaned chart is rescued rather than dropped`() {
        // A chart whose patient exists only in the legacy table: no CRM row to
        // attach to. It must not disappear.
        db.execSQL(
            "INSERT INTO patients (id, tenantId, name, document, createdAt, updatedAt, status, pendingSync) " +
                "VALUES (42, 1, 'Joana Ribeiro', '000', '', '', 'ACTIVE', 0)"
        )
        db.execSQL("INSERT INTO lab_exams (id, patientId, name) VALUES (1, 42, 'Hemograma')")

        runMigration12to13()

        val rescuedId = db.query("SELECT patientId FROM lab_exams WHERE id = 1").use { cursor ->
            assertTrue("the exam must not be dropped", cursor.moveToNext())
            cursor.getLong(0)
        }
        db.query("SELECT name FROM crm_patients WHERE id = ?", arrayOf<Any>(rescuedId)).use { cursor ->
            assertTrue("a CRM row must exist for the rescued chart", cursor.moveToNext())
            // Her name is carried over so the doctor can recognise the record.
            assertEquals("Joana Ribeiro", cursor.getString(0))
        }
    }

    @Test
    fun `clinical insert against a crm patient now succeeds`() {
        insertCrmPatient(id = 5, name = "Ana Lima")
        runMigration12to13()
        db.execSQL("PRAGMA foreign_keys = ON")

        // This is the write that used to fail with a foreign-key violation and
        // reach the doctor as "Falha ao acessar dados locais".
        db.execSQL("INSERT INTO prontuario_documents (patientId, name, uri) VALUES (5, 'TCLE.pdf', 'file://tcle')")

        db.query("SELECT COUNT(*) FROM prontuario_documents WHERE patientId = 5").use { cursor ->
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun `a write for a patient who does not exist is still refused`() {
        runMigration12to13()
        db.execSQL("PRAGMA foreign_keys = ON")

        // The foreign key still has to do its job — it was never the problem,
        // it was simply pointing at the wrong table.
        val threw = runCatching {
            db.execSQL("INSERT INTO vital_signs (patientId, label, value) VALUES (999, 'PA', '120/80')")
        }.isFailure
        assertTrue("a chart must not attach to a patient who does not exist", threw)
    }

    private fun runMigration12to13() {
        val migration = DatabaseModule.migrations().first { it.startVersion == 12 && it.endVersion == 13 }
        db.beginTransaction()
        try {
            migration.migrate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertCrmPatient(id: Long, name: String) {
        db.execSQL(
            "INSERT INTO crm_patients (id, tenantId, name, phone, email, birthDate, stage, " +
                "totalSessions, totalRevenueBrl, lastVisit, nextAppointment, tags, notes, " +
                "referralSource, npsScore, healthInsurance, mainComplaint, createdAt, updatedAt, " +
                "pendingSync, deleted, lastModified) " +
                "VALUES (?, 1, ?, '', '', '', 'ACTIVE', 0, 0.0, '', '', '', '', '', NULL, '', '', '', '', 0, 0, '')",
            arrayOf<Any>(id, name),
        )
    }

    /**
     * The v12 schema, reduced to the tables this migration touches: the two
     * patient tables, and the clinical tables that (wrongly) referenced the
     * legacy one.
     */
    private fun createV12Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `patients` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`tenantId` INTEGER NOT NULL, `name` TEXT NOT NULL, `document` TEXT, " +
                "`createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`pendingSync` INTEGER NOT NULL DEFAULT 0)"
        )
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

        legacyClinicalTables().forEach { db.execSQL(it) }
    }

    private fun legacyClinicalTables(): List<String> {
        fun legacyFk() =
            ", FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
        return listOf(
            "CREATE TABLE `prontuarios` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `summary` TEXT NOT NULL DEFAULT '', " +
                "`mainComplaint` TEXT NOT NULL DEFAULT '', `diagnosis` TEXT NOT NULL DEFAULT '', " +
                "`treatmentPlan` TEXT NOT NULL DEFAULT '', `syncedAt` TEXT, " +
                "`createdAt` TEXT NOT NULL DEFAULT '', `updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `prontuario_entries` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `doctorName` TEXT NOT NULL DEFAULT '', " +
                "`date` TEXT NOT NULL DEFAULT '', `type` TEXT NOT NULL DEFAULT 'EVOLUTION', " +
                "`body` TEXT NOT NULL DEFAULT '', `attachmentsJson` TEXT NOT NULL DEFAULT '[]', " +
                "`syncedAt` TEXT, `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `mtc_assessments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `date` TEXT NOT NULL DEFAULT '', " +
                "`chiefComplaint` TEXT NOT NULL DEFAULT '', `baGangPolarity` TEXT NOT NULL DEFAULT 'UNSET', " +
                "`baGangDepth` TEXT NOT NULL DEFAULT 'UNSET', `baGangTemperature` TEXT NOT NULL DEFAULT 'UNSET', " +
                "`baGangStrength` TEXT NOT NULL DEFAULT 'UNSET', `patternsJson` TEXT NOT NULL DEFAULT '[]', " +
                "`tongueJson` TEXT NOT NULL DEFAULT '{}', `pulseJson` TEXT NOT NULL DEFAULT '{}', " +
                "`bodyMarksJson` TEXT NOT NULL DEFAULT '[]', `flagsCsv` TEXT NOT NULL DEFAULT '', " +
                "`gestationalWeeks` INTEGER, `clinicalImpression` TEXT NOT NULL DEFAULT '', " +
                "`syncedAt` TEXT, `createdAt` TEXT NOT NULL DEFAULT '', `updatedAt` TEXT NOT NULL DEFAULT '', " +
                "`relievingJson` TEXT NOT NULL DEFAULT '[]', `aggravatingJson` TEXT NOT NULL DEFAULT '[]', " +
                "`reviewOfSystemsJson` TEXT NOT NULL DEFAULT '[]', `interrogationNotes` TEXT NOT NULL DEFAULT '', " +
                "`orientations` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `vital_signs` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `label` TEXT NOT NULL, `value` TEXT NOT NULL, " +
                "`recordedAt` TEXT NOT NULL DEFAULT '', `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `lab_exams` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `name` TEXT NOT NULL, `date` TEXT NOT NULL DEFAULT '', " +
                "`resultTag` TEXT NOT NULL DEFAULT 'PENDING', `notes` TEXT NOT NULL DEFAULT '', " +
                "`createdAt` TEXT NOT NULL DEFAULT '', `updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `medications` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `name` TEXT NOT NULL, `info` TEXT NOT NULL DEFAULT '', " +
                "`active` INTEGER NOT NULL DEFAULT 1, `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `allergies` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                "`createdAt` TEXT NOT NULL DEFAULT '', `updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
            "CREATE TABLE `prontuario_documents` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`patientId` INTEGER NOT NULL, `name` TEXT NOT NULL, `uri` TEXT NOT NULL, " +
                "`mimeType` TEXT NOT NULL DEFAULT '', `sizeBytes` INTEGER NOT NULL DEFAULT 0, " +
                "`addedAt` TEXT NOT NULL DEFAULT '', `createdAt` TEXT NOT NULL DEFAULT '', " +
                "`updatedAt` TEXT NOT NULL DEFAULT ''" + legacyFk(),
        )
    }

    private companion object {
        const val DB_NAME = "migration_v13_test.db"
    }
}
