package com.bioacupunt.crm.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration v29 → v30: CRM Extended (Phase 7).
 *
 * Adds: crm_people, crm_organizations, crm_leads, crm_pipelines,
 * crm_pipeline_stages, crm_tasks, crm_activities, crm_tags, crm_identity_map.
 *
 * ADDITIVE ONLY — no columns or tables are removed.
 * The existing crm_patients table is preserved as-is (backward compatible).
 */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // crm_people
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_people (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                personType TEXT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                document TEXT NOT NULL DEFAULT '',
                birthDate TEXT NOT NULL DEFAULT '',
                organizationId INTEGER NOT NULL DEFAULT 0,
                tagsCsv TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                referralSource TEXT NOT NULL DEFAULT '',
                npsScore INTEGER NOT NULL DEFAULT 0,
                healthInsurance TEXT NOT NULL DEFAULT '',
                mainComplaint TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_people_tenantId ON crm_people(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_people_personType ON crm_people(personType)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_people_organizationId ON crm_people(organizationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_people_deleted ON crm_people(deleted)")

        // crm_organizations
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_organizations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                type TEXT NOT NULL DEFAULT 'OTHER',
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                website TEXT NOT NULL DEFAULT '',
                address TEXT NOT NULL DEFAULT '',
                cnpj TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                tagsCsv TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_organizations_tenantId ON crm_organizations(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_organizations_deleted ON crm_organizations(deleted)")

        // crm_leads
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_leads (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                source TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'NEW',
                pipelineId INTEGER NOT NULL DEFAULT 0,
                pipelineStageOrder INTEGER NOT NULL DEFAULT 0,
                assignedTo TEXT NOT NULL DEFAULT '',
                referredBy INTEGER NOT NULL DEFAULT 0,
                mainComplaint TEXT NOT NULL DEFAULT '',
                tagsCsv TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                convertedPatientId INTEGER NOT NULL DEFAULT 0,
                convertedAt TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_leads_tenantId ON crm_leads(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_leads_status ON crm_leads(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_leads_pipelineId ON crm_leads(pipelineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_leads_deleted ON crm_leads(deleted)")

        // crm_pipelines
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_pipelines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_pipelines_tenantId ON crm_pipelines(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_pipelines_deleted ON crm_pipelines(deleted)")

        // crm_pipeline_stages
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_pipeline_stages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                pipelineId INTEGER NOT NULL,
                name TEXT NOT NULL,
                `order` INTEGER NOT NULL,
                color TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_pipeline_stages_tenantId ON crm_pipeline_stages(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_pipeline_stages_pipelineId ON crm_pipeline_stages(pipelineId)")

        // crm_tasks
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'PENDING',
                priority TEXT NOT NULL DEFAULT 'MEDIUM',
                category TEXT NOT NULL DEFAULT 'ADMINISTRATIVE',
                assignedTo TEXT NOT NULL DEFAULT '',
                dueDate TEXT NOT NULL DEFAULT '',
                completedAt TEXT NOT NULL DEFAULT '',
                relationType TEXT NOT NULL DEFAULT 'GENERAL',
                relatedEntityId INTEGER NOT NULL DEFAULT 0,
                tagsCsv TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_tasks_tenantId ON crm_tasks(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_tasks_status ON crm_tasks(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_tasks_dueDate ON crm_tasks(dueDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_tasks_deleted ON crm_tasks(deleted)")

        // crm_activities
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_activities (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                author TEXT NOT NULL DEFAULT '',
                timestamp TEXT NOT NULL,
                relationType TEXT NOT NULL DEFAULT 'GENERAL',
                relatedEntityId INTEGER NOT NULL DEFAULT 0,
                durationMinutes INTEGER NOT NULL DEFAULT 0,
                tagsCsv TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_activities_tenantId ON crm_activities(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_activities_type ON crm_activities(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_activities_timestamp ON crm_activities(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_activities_deleted ON crm_activities(deleted)")

        // crm_tags
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '',
                createdAt TEXT NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_tags_tenantId ON crm_tags(tenantId)")

        // crm_identity_map
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS crm_identity_map (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tenantId INTEGER NOT NULL,
                entityType TEXT NOT NULL,
                crmEntityId INTEGER NOT NULL,
                bioacupuntEntityId INTEGER NOT NULL,
                bioacupuntEntityType TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                lastSyncedAt TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_identity_map_tenantId ON crm_identity_map(tenantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_identity_map_crmEntityId ON crm_identity_map(crmEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_crm_identity_map_bioacupuntEntityId ON crm_identity_map(bioacupuntEntityId)")
    }
}
