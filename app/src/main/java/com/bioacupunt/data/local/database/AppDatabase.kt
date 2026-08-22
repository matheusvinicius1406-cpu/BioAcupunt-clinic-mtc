package com.bioacupunt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.data.local.model.IngestionJobEntity
import com.bioacupunt.data.local.model.PurgeCertificateEntity
import com.bioacupunt.data.local.model.AuditTrailEntity
import com.bioacupunt.biblioteca.data.local.fts.ArticleFtsEntity
import com.bioacupunt.biblioteca.data.local.ArticleTranslationEntity
import com.bioacupunt.biblioteca.data.local.ArticleTranslationDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreSourceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreCitationEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreProvenanceEntity

/**
 * BIOACUPUNT SUPREMO — DATABASE (v19)
 *
 * v18 adiciona as tabelas do MKIS on-device:
 * - knowledge_nodes expandido (enums canônicos, scores, governança)
 * - ingestion_jobs (pipeline state machine)
 * - purge_certificates (LGPD deep delete)
 * - audit_trail (append-only audit log)
 * - vec_knowledge_nodes (sqlite-vec virtual table, criada via migration)
 * - knowledge_fts (FTS5 virtual table, criada via migration)
 *
 * v19 adiciona as tabelas de Educação/Flashcards:
 * - flashcards (cards autorais da médica; os 12 fixos ficam em BuiltinFlashcards.kt)
 * - flashcard_progress (repetição espaçada Leitner-lite, por cardKey)
 *
 * v20 adiciona as tabelas de Farmacologia (BioAcupunt Pharma Library + Smart
 * Prescription):
 * - medicamentos (catálogo ANVISA bulk-importado, identificação) + medicamentos_fts
 * - formulario_medicamento (camada clínica curada pela médica — posologia,
 *   interação, contraindicação, MTC — só o que está aqui alimenta o
 *   PharmaSafetyEngine; o catálogo sozinho nunca é "verificado")
 * - prescricoes (prescrição ligada a paciente, soft delete via `active`)
 *
 * ## Migrações
 * As migrações são gerenciadas centralizadamente em [DatabaseModule].
 * Cada migração é ADDITIVE: nunca remove colunas ou tabelas existentes.
 *
 * ## Histórico de Versões
 * v1-16: Migrações anteriores (ver DatabaseModule)
 * v17: tenantId em transacoes + override do veto clínico
 * v18: MKIS on-device (knowledge_nodes expandido + ingestion_jobs + purge_certificates + audit_trail + sqlite-vec + FTS5)
 * v19: Educação/Flashcards (flashcards + flashcard_progress)
 * v20: Farmacologia (medicamentos + medicamentos_fts + formulario_medicamento + prescricoes)
 * v21: Simulador de Casos Clínicos data-driven (simulated_cases)
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
        com.bioacupunt.educacao.data.local.FlashcardEntity::class,
        com.bioacupunt.educacao.data.local.FlashcardProgressEntity::class,

        // === Farmacologia (v20) ===
        com.bioacupunt.pharma.data.local.MedicamentoEntity::class,
        com.bioacupunt.pharma.data.local.MedicamentoFtsEntity::class,
        com.bioacupunt.pharma.data.local.FormularioMedicamentoEntity::class,
        com.bioacupunt.pharma.data.local.PrescricaoEntity::class,

        // === Simulador de Casos Clínicos (v21) ===
        com.bioacupunt.educacao.data.local.SimulatedCaseEntity::class,

        // === Tradutor automático da Biblioteca (v24) ===
        com.bioacupunt.biblioteca.data.local.ArticleTranslationEntity::class,
        KnowledgeCoreEntityEntity::class,
        KnowledgeCoreRelationEntity::class,
        KnowledgeCoreSourceEntity::class,
        KnowledgeCoreCitationEntity::class,
        KnowledgeCoreEvidenceEntity::class,
        KnowledgeCoreProvenanceEntity::class,

        // === Knowledge Core FTS5 (v26) — created via raw SQL, not Room entity ===

        // === Clinical Workflow Platform (v27) ===
        com.bioacupunt.clinic.data.local.EncounterEntity::class,
        com.bioacupunt.clinic.data.local.ClinicalNoteEntity::class,
        com.bioacupunt.clinic.data.local.TreatmentPlanEntity::class,
        com.bioacupunt.clinic.data.local.FollowUpEntity::class,
        com.bioacupunt.clinic.data.local.StructuredObservationEntity::class,
        com.bioacupunt.clinic.data.local.QuestionnaireResponseEntity::class,

        // === Knowledge Pack Operations (v28) ===
        com.bioacupunt.mtc.knowledge.data.InstalledPackEntity::class,

        // === Clinical Media (v29) ===
        com.bioacupunt.clinic.data.local.ClinicalMediaEntity::class,

        // === CRM Extended (v30) ===
        com.bioacupunt.crm.data.local.CrmPersonEntity::class,
        com.bioacupunt.crm.data.local.CrmOrganizationEntity::class,
        com.bioacupunt.crm.data.local.CrmLeadEntity::class,
        com.bioacupunt.crm.data.local.CrmPipelineEntity::class,
        com.bioacupunt.crm.data.local.PipelineStageEntity::class,
        com.bioacupunt.crm.data.local.CrmTaskEntity::class,
        com.bioacupunt.crm.data.local.CrmActivityEntity::class,
        com.bioacupunt.crm.data.local.CrmTagEntity::class,
        com.bioacupunt.crm.data.local.CrmIdentityMapEntity::class,

        // === Phase 6B: Tongue + Pulse Observations (v31) ===
        com.bioacupunt.clinic.data.local.TongueObservationEntity::class,
        com.bioacupunt.clinic.data.local.PulseObservationEntity::class,
    ],
    version = 31,
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
    abstract fun flashcardDao(): com.bioacupunt.educacao.data.local.FlashcardDao

    // === Farmacologia DAOs (v20) ===
    abstract fun medicamentoDao(): com.bioacupunt.pharma.data.local.MedicamentoDao
    abstract fun medicamentoFtsDao(): com.bioacupunt.pharma.data.local.MedicamentoFtsDao
    abstract fun formularioMedicamentoDao(): com.bioacupunt.pharma.data.local.FormularioMedicamentoDao
    abstract fun prescricaoDao(): com.bioacupunt.pharma.data.local.PrescricaoDao

    // === Simulador de Casos Clínicos DAO (v21) ===
    abstract fun simulatedCaseDao(): com.bioacupunt.educacao.data.local.SimulatedCaseDao

    // === Tradutor automático da Biblioteca DAO (v24) ===
    abstract fun articleTranslationDao(): com.bioacupunt.biblioteca.data.local.ArticleTranslationDao
    abstract fun knowledgeCoreDao(): KnowledgeCoreDao
    // KnowledgeCoreFtsDao is not a Room DAO — uses raw SQL for FTS5 virtual table

    // === Clinical Workflow Platform DAOs (v27) ===
    abstract fun encounterDao(): com.bioacupunt.clinic.data.local.EncounterDao
    abstract fun clinicalNoteDao(): com.bioacupunt.clinic.data.local.ClinicalNoteDao
    abstract fun treatmentPlanDao(): com.bioacupunt.clinic.data.local.TreatmentPlanDao
    abstract fun followUpDao(): com.bioacupunt.clinic.data.local.FollowUpDao
    abstract fun structuredObservationDao(): com.bioacupunt.clinic.data.local.StructuredObservationDao
    abstract fun questionnaireResponseDao(): com.bioacupunt.clinic.data.local.QuestionnaireResponseDao

    // === Knowledge Pack Operations DAOs (v28) ===
    abstract fun installedPackDao(): com.bioacupunt.mtc.knowledge.data.InstalledPackDao

    // === Clinical Media DAOs (v29) ===
    abstract fun clinicalMediaDao(): com.bioacupunt.clinic.data.local.ClinicalMediaDao

    // === CRM Extended DAOs (v30) ===
    abstract fun crmPersonDao(): com.bioacupunt.crm.data.local.CrmPersonDao
    abstract fun crmOrganizationDao(): com.bioacupunt.crm.data.local.CrmOrganizationDao
    abstract fun crmLeadDao(): com.bioacupunt.crm.data.local.CrmLeadDao
    abstract fun crmPipelineDao(): com.bioacupunt.crm.data.local.CrmPipelineDao
    abstract fun pipelineStageDao(): com.bioacupunt.crm.data.local.PipelineStageDao
    abstract fun crmTaskDao(): com.bioacupunt.crm.data.local.CrmTaskDao
    abstract fun crmActivityDao(): com.bioacupunt.crm.data.local.CrmActivityDao
    abstract fun crmTagDao(): com.bioacupunt.crm.data.local.CrmTagDao
    abstract fun crmIdentityMapDao(): com.bioacupunt.crm.data.local.CrmIdentityMapDao

    // === Phase 6B: Tongue + Pulse Observation DAOs (v31) ===
    abstract fun tongueObservationDao(): com.bioacupunt.clinic.data.local.TongueObservationDao
    abstract fun pulseObservationDao(): com.bioacupunt.clinic.data.local.PulseObservationDao
}
