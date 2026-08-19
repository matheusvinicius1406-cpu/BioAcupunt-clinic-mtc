package com.bioacupunt.clinic.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration v26 â†’ v27: Clinical Workflow Platform (Phase 5)
 *
 * Adds:
 * - encounters
 * - clinical_notes
 * - treatment_plans
 * - follow_ups
 * - structured_observations
 * - questionnaire_responses
 *
 * Additive only â€” no columns removed, no tables dropped.
 */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // encounters
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS encounters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                status TEXT NOT NULL,
                type TEXT NOT NULL,
                startedAt TEXT NOT NULL DEFAULT '',
                endedAt TEXT NOT NULL DEFAULT '',
                practitionerId TEXT NOT NULL DEFAULT '',
                reason TEXT NOT NULL DEFAULT '',
                appointmentId INTEGER,
                currentAssessmentId INTEGER,
                currentNoteId INTEGER,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (patientId) REFERENCES crm_patients(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_encounters_tenantId ON encounters(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_encounters_patientId ON encounters(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_encounters_status ON encounters(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_encounters_patientId_startedAt ON encounters(patientId, startedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_encounters_updatedAt ON encounters(updatedAt)")

        // clinical_notes
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS clinical_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                format TEXT NOT NULL,
                subjective TEXT NOT NULL DEFAULT '',
                objective TEXT NOT NULL DEFAULT '',
                assessment TEXT NOT NULL DEFAULT '',
                plan TEXT NOT NULL DEFAULT '',
                mtcAssessmentSummary TEXT NOT NULL DEFAULT '',
                referencesJson TEXT NOT NULL DEFAULT '[]',
                status TEXT NOT NULL,
                createdBy TEXT NOT NULL DEFAULT '',
                finalizedBy TEXT,
                finalizedAt TEXT,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (encounterId) REFERENCES encounters(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_notes_tenantId ON clinical_notes(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_notes_patientId ON clinical_notes(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_notes_encounterId ON clinical_notes(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_notes_status ON clinical_notes(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_clinical_notes_updatedAt ON clinical_notes(updatedAt)")

        // treatment_plans
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS treatment_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                goals TEXT NOT NULL DEFAULT '',
                principles TEXT NOT NULL DEFAULT '',
                itemsJson TEXT NOT NULL DEFAULT '[]',
                frequency TEXT NOT NULL DEFAULT '',
                duration TEXT NOT NULL DEFAULT '',
                followUpRecommendation TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (encounterId) REFERENCES encounters(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_treatment_plans_tenantId ON treatment_plans(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_treatment_plans_patientId ON treatment_plans(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_treatment_plans_encounterId ON treatment_plans(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_treatment_plans_status ON treatment_plans(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_treatment_plans_updatedAt ON treatment_plans(updatedAt)")

        // follow_ups
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS follow_ups (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                encounterId INTEGER,
                scheduledAt TEXT NOT NULL DEFAULT '',
                reason TEXT NOT NULL DEFAULT '',
                expectedFindings TEXT NOT NULL DEFAULT '',
                actualFindings TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL,
                completedAt TEXT,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (patientId) REFERENCES crm_patients(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_tenantId ON follow_ups(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_patientId ON follow_ups(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_status ON follow_ups(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_scheduledAt ON follow_ups(scheduledAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_updatedAt ON follow_ups(updatedAt)")

        // structured_observations
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS structured_observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                encounterId INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                type TEXT NOT NULL,
                content TEXT NOT NULL,
                structuredDataJson TEXT NOT NULL DEFAULT '{}',
                status TEXT NOT NULL,
                source TEXT NOT NULL,
                sourceSpan TEXT,
                confidence REAL,
                reviewedBy TEXT,
                reviewedAt TEXT,
                confirmedBy TEXT,
                confirmedAt TEXT,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (encounterId) REFERENCES encounters(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_tenantId ON structured_observations(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_patientId ON structured_observations(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_encounterId ON structured_observations(encounterId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_type ON structured_observations(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_status ON structured_observations(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_source ON structured_observations(source)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_structured_observations_updatedAt ON structured_observations(updatedAt)")

        // questionnaire_responses
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS questionnaire_responses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                questionnaireId TEXT NOT NULL,
                questionnaireVersion INTEGER NOT NULL,
                patientId INTEGER NOT NULL,
                encounterId INTEGER,
                answersJson TEXT NOT NULL DEFAULT '{}',
                status TEXT NOT NULL,
                createdAt TEXT NOT NULL DEFAULT '',
                updatedAt TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (patientId) REFERENCES crm_patients(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaire_responses_tenantId ON questionnaire_responses(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaire_responses_patientId ON questionnaire_responses(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaire_responses_questionnaireId ON questionnaire_responses(questionnaireId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaire_responses_status ON questionnaire_responses(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaire_responses_updatedAt ON questionnaire_responses(updatedAt)")
    }
}

