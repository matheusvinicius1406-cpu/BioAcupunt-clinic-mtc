package com.bioacupunt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.data.local.model.IngestionJobEntity
import com.bioacupunt.data.local.model.PurgeCertificateEntity
import com.bioacupunt.data.local.model.AuditTrailEntity
import com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity

/**
 * BIOACUPUNT SUPREMO — DATABASE (v18)
 *
 * v18 adiciona as tabelas do MKIS on-device:
 * - knowledge_nodes expandido (enums canônicos, scores, governança)
 * - ingestion_jobs (pipeline state machine)
 * - purge_certificates (LGPD deep delete)
 * - audit_trail (append-only audit log)
 * - vec_knowledge_nodes (sqlite-vec virtual table, criada via migration)
 * - knowledge_fts (FTS5 virtual table, criada via migration)
 *
 * ## Migrações
 * As migrações são gerenciadas centralizadamente em [DatabaseModule].
 * Cada migração é ADDITIVE: nunca remove colunas ou tabelas existentes.
 *
 * ## Histórico de Versões
 * v1-16: Migrações anteriores (ver DatabaseModule)
 * v17: tenantId em transacoes + override do veto clínico
 * v18: MKIS on-device (knowledge_nodes expandido + ingestion_jobs + purge_certificates + audit_trail + sqlite-vec + FTS5)
 */
@Database(
    entities = [
        // === MKIS Core ===
        KnowledgeNodeEntity::class,
        IngestionJobEntity::class,
        PurgeCertificateEntity::class,
        AuditTrailEntity::class,

        // === Bounded Contexts Existentes ===
        com.bioacupunt.patient.data.local.PatientEntity::class,
        com.bioacupunt.sync.data.local.SyncQueueEntity::class,
        com.bioacupunt.crm.data.local.CrmPatientEntity::class,
        com.bioacupunt.agenda.data.local.AppointmentEntity::class,
        com.bioacupunt.financeiro.data.local.TransacaoEntity::class,
        com.bioacupunt.prontuario.data.local.ProntuarioEntity::class,
        com.bioacupunt.prontuario.data.local.ProntuarioEntryEntity::class,
        com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity::class,
        com.bioacupunt.relatorios.data.local.ReportEntity::class,
        com.bioacupunt.prontuario.data.local.MtcAssessmentEntity::class,
        com.bioacupunt.prontuario.data.local.VitalSignEntity::class,
        com.bioacupunt.prontuario.data.local.LabExamEntity::class,
        com.bioacupunt.prontuario.data.local.MedicationEntity::class,
        com.bioacupunt.prontuario.data.local.AllergyEntity::class,
        com.bioacupunt.prontuario.data.local.ProntuarioDocumentEntity::class,
        com.bioacupunt.biblioteca.data.local.FavoriteArticleEntity::class,
        com.bioacupunt.sync.data.local.SyncStateEntity::class,
        com.bioacupunt.sync.data.local.SyncConflictEntity::class,
        com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity::class,
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // === MKIS DAOs ===
    abstract fun knowledgeNodeDao(): KnowledgeNodeDao
    abstract fun ingestionJobDao(): IngestionJobDao
    abstract fun purgeCertificateDao(): PurgeCertificateDao
    abstract fun auditTrailDao(): AuditTrailDao

    // === DAOs Existentes ===
    abstract fun patientDao(): com.bioacupunt.patient.data.local.PatientDao
    abstract fun syncQueueDao(): com.bioacupunt.sync.data.local.SyncQueueDao
    abstract fun crmPatientDao(): com.bioacupunt.crm.data.local.CrmPatientDao
    abstract fun appointmentDao(): com.bioacupunt.agenda.data.local.AppointmentDao
    abstract fun transacaoDao(): com.bioacupunt.financeiro.data.local.TransacaoDao
    abstract fun prontuarioDao(): com.bioacupunt.prontuario.data.local.ProntuarioDao
    abstract fun bibliotecaDao(): com.bioacupunt.biblioteca.data.local.BibliotecaDao
    abstract fun reportDao(): com.bioacupunt.relatorios.data.local.ReportDao
    abstract fun mtcAssessmentDao(): com.bioacupunt.prontuario.data.local.MtcAssessmentDao
    abstract fun exameDao(): com.bioacupunt.prontuario.data.local.ExameDao
    abstract fun prontuarioDocumentDao(): com.bioacupunt.prontuario.data.local.ProntuarioDocumentDao
    abstract fun favoriteArticleDao(): com.bioacupunt.biblioteca.data.local.FavoriteArticleDao
    abstract fun syncStateDao(): com.bioacupunt.sync.data.local.SyncStateDao
    abstract fun syncConflictDao(): com.bioacupunt.sync.data.local.SyncConflictDao
    abstract fun articleSearchDao(): com.bioacupunt.biblioteca.data.local.dao.ArticleSearchDao
}
