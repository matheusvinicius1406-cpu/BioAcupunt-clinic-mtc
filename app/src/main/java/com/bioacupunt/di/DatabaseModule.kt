package com.bioacupunt.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bioacupunt.data.local.database.AppDatabase
import java.io.File

object DatabaseModule {

    private const val DB_NAME = "bioacupunt_db"
    private const val BACKUP_DIR_NAME = "db_backups"

    // MUST match @Database(version = …) on AppDatabase. Kept as a plain
    // constant on purpose: reading the version by reflecting the @Database
    // annotation at runtime (AppDatabase::class.java.getAnnotation(...)) returns
    // null on device — Room's @Database is consumed by the compile-time
    // annotation processor and is not retained for runtime reflection — which
    // crashed every database access with "AppDatabase must be annotated with
    // @Database".
    private const val DB_VERSION = 12

    private var initialized = false
    private lateinit var instance: AppDatabase

    fun provideAppDatabase(context: Context): AppDatabase {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val appContext = context.applicationContext
                    val databaseFile = appContext.getDatabasePath(DB_NAME)
                    val existingVersion = if (databaseFile.exists()) {
                        runCatching { getCurrentDbVersion(databaseFile) }.getOrNull()
                    } else null

                    if (existingVersion != null && existingVersion < DB_VERSION) {
                        backupDatabase(appContext)
                    }

                    val migrations = buildMigrations()
                    instance = Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                        .addMigrations(*migrations.toTypedArray())
                        .build()
                    initialized = true
                }
            }
        }
        return instance
    }

    private fun getCurrentDbVersion(databaseFile: File): Int {
        var version = 0
        databaseFile.inputStream().use { input ->
            val buffer = ByteArray(32)
            input.skip(24)
            val read = input.read(buffer, 0, 4)
            if (read == 4) {
                version = ((buffer[0].toInt() and 0xFF) shl 24) or
                    ((buffer[1].toInt() and 0xFF) shl 16) or
                    ((buffer[2].toInt() and 0xFF) shl 8) or
                    (buffer[3].toInt() and 0xFF)
            }
        }
        return version
    }

    private fun backupDatabase(context: Context) {
        runCatching {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@runCatching
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists()) backupDir.mkdirs()
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$DB_NAME-$timestamp.bak")
            dbFile.copyTo(backupFile, overwrite = true)
            arrayOf("-wal", "-shm").forEach { suffix ->
                val src = File(dbFile.parentFile, dbFile.name + suffix)
                val dst = File(backupDir, "$DB_NAME-$timestamp.bak$suffix")
                if (src.exists()) {
                    src.copyTo(dst, overwrite = true)
                }
            }
            deleteOldBackups(backupDir)
            com.bioacupunt.observability.AppLogger.i("DatabaseModule", "Database backup created: ${backupFile.absolutePath}")
        }.onFailure { e ->
            com.bioacupunt.observability.AppLogger.e("DatabaseModule", "Database backup failed", e)
        }
    }

    private fun deleteOldBackups(backupDir: File) {
        runCatching {
            val backups = backupDir.listFiles()?.sortedBy { it.name } ?: emptyList()
            val toDelete = backups.dropLast(5)
            toDelete.forEach { it.delete() }
        }
    }

    private fun buildMigrations(): List<androidx.room.migration.Migration> {
        val current = DB_VERSION
        val migrations = mutableListOf<androidx.room.migration.Migration>()

        if (current >= 2) migrations.add(MIGRATION_1_2)
        if (current >= 3) migrations.add(MIGRATION_2_3)
        if (current >= 4) migrations.add(MIGRATION_3_4)
        if (current >= 5) migrations.add(MIGRATION_4_5)
        if (current >= 6) migrations.add(MIGRATION_5_6)
        if (current >= 7) migrations.add(MIGRATION_6_7)
        if (current >= 8) migrations.add(MIGRATION_7_8)
        if (current >= 9) migrations.add(MIGRATION_8_9)
        if (current >= 10) migrations.add(MIGRATION_9_10)
        if (current >= 11) migrations.add(MIGRATION_10_11)
        if (current >= 12) migrations.add(MIGRATION_11_12)
        return migrations
    }

    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE patients ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE patients ADD COLUMN tenantId INTEGER NOT NULL DEFAULT 1")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_tenantId ON patients(tenantId)")
        }
    }

    private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN lastError TEXT")
        }
    }

    private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE sync_queue ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN sessionNumber INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE appointments ADD COLUMN valueBrl REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE appointments ADD COLUMN paid INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Adds the structured TCM assessment ("Prontuário Supremo").
     *
     * Additive only: no existing table is touched, so no chart written by v8 can be
     * lost by upgrading. The column types and the index names must match exactly what
     * Room derives from [com.bioacupunt.prontuario.data.local.MtcAssessmentEntity],
     * or Room throws an IllegalStateException on open — which, given this app already
     * died once at startup, is a failure mode worth being paranoid about.
     */
    private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `mtc_assessments` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL DEFAULT '',
                    `chiefComplaint` TEXT NOT NULL DEFAULT '',
                    `baGangPolarity` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangDepth` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangTemperature` TEXT NOT NULL DEFAULT 'UNSET',
                    `baGangStrength` TEXT NOT NULL DEFAULT 'UNSET',
                    `patternsJson` TEXT NOT NULL DEFAULT '[]',
                    `tongueJson` TEXT NOT NULL DEFAULT '{}',
                    `pulseJson` TEXT NOT NULL DEFAULT '{}',
                    `bodyMarksJson` TEXT NOT NULL DEFAULT '[]',
                    `flagsCsv` TEXT NOT NULL DEFAULT '',
                    `gestationalWeeks` INTEGER,
                    `clinicalImpression` TEXT NOT NULL DEFAULT '',
                    `syncedAt` TEXT,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_patientId` ON `mtc_assessments` (`patientId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_date` ON `mtc_assessments` (`date`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_mtc_assessments_updatedAt` ON `mtc_assessments` (`updatedAt`)",
            )
        }
    }

    /**
     * Adds the "Exames" (vitals/labs/medications/allergies) and "Documentos" tables.
     * Additive only, same paranoia as MIGRATION_8_9: column types and index names must
     * match exactly what Room derives from the corresponding entities.
     */
    private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vital_signs` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `label` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    `recordedAt` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vital_signs_patientId` ON `vital_signs` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_vital_signs_recordedAt` ON `vital_signs` (`recordedAt`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `lab_exams` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `date` TEXT NOT NULL DEFAULT '',
                    `resultTag` TEXT NOT NULL DEFAULT 'PENDING',
                    `notes` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_exams_patientId` ON `lab_exams` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_exams_date` ON `lab_exams` (`date`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `medications` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `info` TEXT NOT NULL DEFAULT '',
                    `active` INTEGER NOT NULL DEFAULT 1,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_patientId` ON `medications` (`patientId`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `allergies` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `description` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_allergies_patientId` ON `allergies` (`patientId`)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `prontuario_documents` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `patientId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL DEFAULT '',
                    `sizeBytes` INTEGER NOT NULL DEFAULT 0,
                    `addedAt` TEXT NOT NULL DEFAULT '',
                    `createdAt` TEXT NOT NULL DEFAULT '',
                    `updatedAt` TEXT NOT NULL DEFAULT '',
                    FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_prontuario_documents_patientId` ON `prontuario_documents` (`patientId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_prontuario_documents_addedAt` ON `prontuario_documents` (`addedAt`)")
        }
    }

    /** Adds article bookmarking ("favoritos" in the Biblioteca screen). */
    private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `favorite_articles` (
                    `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `articleId` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_articles_articleId` ON `favorite_articles` (`articleId`)")
        }
    }

    /** Adds the Atendimento wizard's extra MTC-assessment fields (additive columns only). */
    private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN relievingJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN aggravatingJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN reviewOfSystemsJson TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN interrogationNotes TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE mtc_assessments ADD COLUMN orientations TEXT NOT NULL DEFAULT ''")
        }
    }
}
