package com.bioacupunt.crm.domain.model

/**
 * Person type — distinguishes roles without creating separate tables for each.
 */
enum class PersonType(val label: String) {
    PATIENT("Paciente"),
    PROFESSIONAL("Profissional"),
    REFERRER("Referenciador"),
    CAREGIVER("Cuidador"),
    CONTACT("Contato"),
    LEAD("Lead"),
}

/**
 * A person in the CRM system.
 * When personType == PATIENT, there is a corresponding BioAcupunt Patient record.
 * The identity map links CRM Person ↔ BioAcupunt Patient.
 */
data class CrmPerson(
    val id: Long = 0,
    val tenantId: Long,
    val personType: PersonType,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val document: String = "",
    val birthDate: String = "",
    val organizationId: Long? = null,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val referralSource: String = "",
    val npsScore: Int? = null,
    val healthInsurance: String = "",
    val mainComplaint: String = "",
    val status: String = "ACTIVE",
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * Organization types.
 */
enum class OrganizationType(val label: String) {
    CLINIC("Clínica"),
    HOSPITAL("Hospital"),
    COMPANY("Empresa"),
    PARTNER("Parceiro"),
    SUPPLIER("Fornecedor"),
    REFERRER_ORGANIZATION("Organização Referenciadora"),
    INSURANCE("Seguradora"),
    OTHER("Outro"),
}

/**
 * An organization in the CRM.
 */
data class CrmOrganization(
    val id: Long = 0,
    val tenantId: Long,
    val type: OrganizationType = OrganizationType.OTHER,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val cnpj: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * Lead status — tracks progression from first contact to patient.
 */
enum class LeadStatus(val label: String) {
    NEW("Novo"),
    CONTACTED("Contato inicial"),
    QUALIFIED("Qualificado"),
    SCHEDULED("Agendado"),
    CONVERTED("Convertido"),
    LOST("Perdido"),
    ARCHIVED("Arquivado"),
}

/**
 * A potential patient (lead) in the CRM pipeline.
 */
data class CrmLead(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val source: String = "",
    val status: LeadStatus = LeadStatus.NEW,
    val pipelineId: Long? = null,
    val pipelineStageOrder: Int = 0,
    val assignedTo: String = "",
    val referredBy: Long? = null,
    val mainComplaint: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    /** After conversion, links to the created Patient */
    val convertedPatientId: Long? = null,
    val convertedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * A pipeline stage — e.g. "Novo", "Contato", "Qualificado", "Agendado".
 */
data class PipelineStage(
    val id: Long = 0,
    val tenantId: Long = 0,
    val pipelineId: Long = 0,
    val name: String,
    val order: Int,
    val color: String = "",
    val createdAt: String = "",
)

/**
 * A pipeline — ordered stages for tracking leads/opportunities.
 */
data class CrmPipeline(
    val id: Long = 0,
    val tenantId: Long = 0,
    val name: String,
    val description: String = "",
    val isDefault: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * Task status.
 */
enum class TaskStatus(val label: String) {
    PENDING("Pendente"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada"),
    OVERDOWN("Atrasada"),
}

/**
 * Task priority.
 */
enum class TaskPriority(val label: String) {
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta"),
    URGENT("Urgente"),
}

/**
 * What a task relates to.
 */
enum class TaskRelation(val label: String) {
    PATIENT("Paciente"),
    LEAD("Lead"),
    ENCOUNTER("Atendimento"),
    FOLLOW_UP("Retorno"),
    ORGANIZATION("Organização"),
    PIPELINE("Pipeline"),
    COMMUNICATION("Comunicação"),
    GENERAL("Geral"),
}

/**
 * A CRM task — can be clinical, administrative, follow-up, or operational.
 */
data class CrmTask(
    val id: Long = 0,
    val tenantId: Long,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "ADMINISTRATIVE",
    val assignedTo: String = "",
    val dueDate: String = "",
    val completedAt: String? = null,
    /** What entity this task relates to */
    val relationType: TaskRelation = TaskRelation.GENERAL,
    val relatedEntityId: Long? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * Activity type — what happened in a CRM interaction.
 */
enum class CrmActivityType(val label: String) {
    CALL("Ligação"),
    EMAIL("E-mail"),
    MESSAGE("Mensagem"),
    MEETING("Reunião"),
    NOTE("Nota"),
    TASK("Tarefa"),
    APPOINTMENT("Agendamento"),
    ENCOUNTER("Atendimento"),
    FOLLOW_UP("Retorno"),
    REFERRAL("Referência"),
    SYSTEM("Sistema"),
}

/**
 * A CRM activity — logged interaction or event.
 */
data class CrmActivity(
    val id: Long = 0,
    val tenantId: Long,
    val type: CrmActivityType,
    val title: String,
    val description: String = "",
    val author: String = "",
    val timestamp: String = "",
    /** What entity this activity relates to */
    val relationType: TaskRelation = TaskRelation.GENERAL,
    val relatedEntityId: Long? = null,
    val durationMinutes: Int? = null,
    val tags: List<String> = emptyList(),
    val createdAt: String = "",
    val deletedAt: String? = null,
)

/**
 * A CRM tag — lightweight classification for people, leads, tasks, etc.
 */
data class CrmTag(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val color: String = "",
    val createdAt: String = "",
)

/**
 * Identity map linking CRM entities to BioAcupunt clinical entities.
 * Ensures single source of truth: one patient, many views.
 */
data class CrmIdentityMap(
    val id: Long = 0,
    val tenantId: Long,
    val entityType: String,
    val crmEntityId: Long,
    val bioacupuntEntityId: Long,
    val bioacupuntEntityType: String,
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastSyncedAt: String? = null,
)

/**
 * CRM timeline event — extends ClinicalTimelineEvent with CRM-specific types.
 */
enum class CrmTimelineEventType(val label: String) {
    CRM_ACTIVITY("Atividade CRM"),
    CRM_TASK_CREATED("Tarefa criada"),
    CRM_TASK_COMPLETED("Tarefa concluída"),
    CRM_LEAD_CREATED("Lead criado"),
    CRM_LEAD_CONVERTED("Lead convertido"),
    CRM_PIPELINE_CHANGED("Pipeline alterado"),
    CRM_NOTE_CREATED("Nota criada"),
    CRM_REFERRAL("Referência"),
    CRM_COMMUNICATION("Comunicação"),
}

/**
 * Patient operational status — based on activity, not clinical assessment.
 */
enum class PatientOperationalStatus(val label: String) {
    ACTIVE("Ativo"),
    AT_RISK("Em risco"),
    INACTIVE("Inativo"),
}

/**
 * Patient 360 view — unified patient information combining clinical + CRM.
 */
data class Patient360(
    val patientId: Long,
    val tenantId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val personType: PersonType = PersonType.PATIENT,
    val operationalStatus: PatientOperationalStatus = PatientOperationalStatus.ACTIVE,
    /** Clinical data */
    val sessionCount: Int = 0,
    val lastEncounterDate: String? = null,
    val nextAppointment: String? = null,
    val currentAssessment: String = "",
    val chiefComplaint: String = "",
    /** CRM data */
    val totalActivities: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val lastActivityDate: String? = null,
    val lastContactDate: String? = null,
    val tags: List<String> = emptyList(),
    val referralSource: String = "",
    /** Intelligence */
    val longitudinalSummary: String = "",
    val missingData: List<String> = emptyList(),
)

/**
 * Default CRM pipelines for a new tenant.
 */
object DefaultPipelines {
    val PATIENT_CARE = CrmPipeline(
        name = "Cuidado do Paciente",
        description = "Fluxo de atendimento do paciente",
        isDefault = true,
    )

    val LEAD_CONVERSION = CrmPipeline(
        name = "Conversão de Leads",
        description = "Fluxo de qualificação e conversão de leads",
    )
}

val DEFAULT_PATIENT_CARE_STAGES = listOf(
    PipelineStage(name = "Primeiro contato", order = 0),
    PipelineStage(name = "Avaliação inicial", order = 1),
    PipelineStage(name = "Em tratamento", order = 2),
    PipelineStage(name = "Manutenção", order = 3),
    PipelineStage(name = "Alta", order = 4),
)

val DEFAULT_LEAD_STAGES = listOf(
    PipelineStage(name = "Novo", order = 0),
    PipelineStage(name = "Contato", order = 1),
    PipelineStage(name = "Qualificado", order = 2),
    PipelineStage(name = "Agendado", order = 3),
    PipelineStage(name = "Convertido", order = 4),
    PipelineStage(name = "Perdido", order = 5),
)
