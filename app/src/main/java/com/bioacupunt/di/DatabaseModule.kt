package com.bioacupunt.di

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bioacupunt.data.local.database.AppDatabase
import java.io.File

object DatabaseModule {

    private const val DB_NAME = "bioacupunt_db"
    private const val BACKUP_DIR_NAME = "db_backups"

    private var initialized = false
    private lateinit var instance: AppDatabase

    private val schemaVersion: Int by lazy {
        checkNotNull(AppDatabase::class.java.getAnnotation(Database::class.java)) {
            "AppDatabase must be annotated with @Database"
        }.version
    }

    fun provideAppDatabase(context: Context): AppDatabase {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val appContext = context.applicationContext
                    val databaseFile = appContext.getDatabasePath(DB_NAME)
                    val existingVersion = if (databaseFile.exists()) {
                        runCatching { getCurrentDbVersion(databaseFile) }.getOrNull()
                    } else null

                    if (existingVersion != null && existingVersion < schemaVersion) {
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
        val current = schemaVersion
        val migrations = mutableListOf<androidx.room.migration.Migration>()

        if (current >= 2) migrations.add(MIGRATION_1_2)
        if (current >= 3) migrations.add(MIGRATION_2_3)
        if (current >= 4) migrations.add(MIGRATION_3_4)
        if (current >= 5) migrations.add(MIGRATION_4_5)
        if (current >= 6) migrations.add(MIGRATION_5_6)
        if (current >= 7) migrations.add(MIGRATION_6_7)
        if (current >= 8) migrations.add(MIGRATION_7_8)
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
}
