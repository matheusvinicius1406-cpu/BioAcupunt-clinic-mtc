package com.bioacupunt.clinic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration v30 → v31: Tongue + Pulse Observations (Phase 6B).
 *
 * Adds: tongue_observations, pulse_observations.
 *
 * ADDITIVE ONLY — no columns or tables are removed.
 * No DEFAULT in CREATE TABLE — Room validates against @ColumnInfo defaults.
 */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // tongue_observations
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tongue_observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL DEFAULT 0,
                mediaId INTEGER NOT NULL DEFAULT 0,
                observationId INTEGER NOT NULL DEFAULT 0,
                bodyColor TEXT NOT NULL DEFAULT '',
                bodyColorNotes TEXT NOT NULL DEFAULT '',
                shape TEXT NOT NULL DEFAULT '',
                shapeNotes TEXT NOT NULL DEFAULT '',
                coating TEXT NOT NULL DEFAULT '',
                coatingNotes TEXT NOT NULL DEFAULT '',
                moisture TEXT NOT NULL DEFAULT '',
                moistureNotes TEXT NOT NULL DEFAULT '',
                cracks TEXT NOT NULL DEFAULT '',
                marks TEXT NOT NULL DEFAULT '',
                movement TEXT NOT NULL DEFAULT '',
                specialFindings TEXT NOT NULL DEFAULT '',
                regionTip TEXT NOT NULL DEFAULT '',
                regionCenter TEXT NOT NULL DEFAULT '',
                regionRoot TEXT NOT NULL DEFAULT '',
                regionLeft TEXT NOT NULL DEFAULT '',
                regionRight TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'DRAFT',
                source TEXT NOT NULL DEFAULT 'MANUAL',
                visionModelName TEXT NOT NULL DEFAULT '',
                visionModelVersion TEXT NOT NULL DEFAULT '',
                visionConfidence REAL NOT NULL DEFAULT 0.0,
                preprocessingVersion TEXT NOT NULL DEFAULT '',
                reviewedBy TEXT NOT NULL DEFAULT '',
                reviewedAt TEXT NOT NULL DEFAULT '',
                confirmedBy TEXT NOT NULL DEFAULT '',
                confirmedAt TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tongue_observations_tenantId ON tongue_observations(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tongue_observations_patientId ON tongue_observations(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tongue_observations_encounterId ON tongue_observations(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tongue_observations_status ON tongue_observations(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tongue_observations_mediaId ON tongue_observations(mediaId)")

        // pulse_observations
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pulse_observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL DEFAULT 0,
                observationId INTEGER NOT NULL DEFAULT 0,
                depth TEXT NOT NULL DEFAULT '',
                rate INTEGER NOT NULL DEFAULT 0,
                strength TEXT NOT NULL DEFAULT '',
                width TEXT NOT NULL DEFAULT '',
                quality TEXT NOT NULL DEFAULT '',
                qualityNotes TEXT NOT NULL DEFAULT '',
                leftCun TEXT NOT NULL DEFAULT '',
                leftGuan TEXT NOT NULL DEFAULT '',
                leftChi TEXT NOT NULL DEFAULT '',
                rightCun TEXT NOT NULL DEFAULT '',
                rightGuan TEXT NOT NULL DEFAULT '',
                rightChi TEXT NOT NULL DEFAULT '',
                featuresJson TEXT NOT NULL DEFAULT '[]',
                status TEXT NOT NULL DEFAULT 'DRAFT',
                source TEXT NOT NULL DEFAULT 'MANUAL',
                reviewedBy TEXT NOT NULL DEFAULT '',
                reviewedAt TEXT NOT NULL DEFAULT '',
                confirmedBy TEXT NOT NULL DEFAULT '',
                confirmedAt TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pulse_observations_tenantId ON pulse_observations(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pulse_observations_patientId ON pulse_observations(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pulse_observations_encounterId ON pulse_observations(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pulse_observations_status ON pulse_observations(status)")
    }
}
