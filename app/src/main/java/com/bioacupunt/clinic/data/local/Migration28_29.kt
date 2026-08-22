package com.bioacupunt.clinic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration v28 → v29: Clinical Media (Phase 6B).
 *
 * Adds the `clinical_media` table for tracking images, audio, video, and documents
 * linked to patients and encounters. Binary files are stored in secure app-internal
 * storage; this table stores metadata only.
 *
 * ADDITIVE ONLY — no columns or tables are removed.
 */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS clinical_media (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL DEFAULT 0,
                type TEXT NOT NULL,
                uri TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                originalName TEXT NOT NULL DEFAULT '',
                sizeBytes INTEGER NOT NULL DEFAULT 0,
                hash TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL,
                status TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                processingVersion TEXT NOT NULL DEFAULT '',
                capturedAt TEXT NOT NULL DEFAULT '',
                capturedBy TEXT NOT NULL DEFAULT '',
                deviceInfo TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_media_tenantId ON clinical_media(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_media_patientId ON clinical_media(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_media_encounterId ON clinical_media(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_media_status ON clinical_media(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_media_deleted ON clinical_media(deleted)")
    }
}
