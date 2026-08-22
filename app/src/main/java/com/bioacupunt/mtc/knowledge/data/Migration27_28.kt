package com.bioacupunt.mtc.knowledge.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration v27 → v28: Knowledge Pack Operations (Phase 6A)
 *
 * Adds:
 * - installed_packs (pack installation lifecycle tracking)
 *
 * Additive only — no columns removed, no tables dropped.
 */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // installed_packs
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS installed_packs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                packId TEXT NOT NULL,
                version TEXT NOT NULL,
                status TEXT NOT NULL,
                manifestJson TEXT NOT NULL DEFAULT '',
                checksum TEXT NOT NULL DEFAULT '',
                installedAt TEXT NOT NULL DEFAULT '',
                activatedAt TEXT,
                deactivatedAt TEXT,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                deletedAt TEXT
            )
            """.trimIndent()
        )

        // Indices for multi-tenant isolation and query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS index_installed_packs_tenantId ON installed_packs (tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_installed_packs_packId ON installed_packs (packId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_installed_packs_status ON installed_packs (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_installed_packs_deleted ON installed_packs (deleted)")
    }
}
