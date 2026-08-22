package com.bioacupunt.crm.domain.model

// ═════════════════════════════════════════════════════════════════════
// OPPORTUNITY
// ═════════════════════════════════════════════════════════════════════

enum class OpportunityType(val label: String) {
    NEW_PATIENT("Novo paciente"),
    REFERRAL("Referência"),
    PARTNERSHIP("Parceria"),
    SERVICE("Serviço"),
    PROGRAM("Programa"),
    OTHER("Outro"),
}

enum class OpportunityStatus(val label: String) {
    OPEN("Aberta"),
    WON("Ganha"),
    LOST("Perdida"),
    CANCELLED("Cancelada"),
}

data class CrmOpportunity(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val type: OpportunityType = OpportunityType.NEW_PATIENT,
    val status: OpportunityStatus = OpportunityStatus.OPEN,
    val pipelineId: Long? = null,
    val stageOrder: Int = 0,
    val value: Double = 0.0,
    val currency: String = "BRL",
    val leadId: Long? = null,
    val patientId: Long? = null,
    val organizationId: Long? = null,
    val assignedTo: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val closedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

// ═════════════════════════════════════════════════════════════════════
// CRM NOTE TYPE (extends existing CrmNote in CrmModels.kt)
// ═════════════════════════════════════════════════════════════════════

enum class CrmNoteType(val label: String) {
    GENERAL("Geral"),
    FOLLOW_UP("Follow-up"),
    ALERT("Alerta"),
    RELATIONSHIP("Relacionamento"),
    OPERATIONAL("Operacional"),
    REFERRAL("Referência"),
}

// ═════════════════════════════════════════════════════════════════════
// REFERRAL
// ═════════════════════════════════════════════════════════════════════

enum class ReferralStatus(val label: String) {
    PENDING("Pendente"),
    CONTACTED("Contato"),
    SCHEDULED("Agendado"),
    CONVERTED("Convertido"),
    DECLINED("Recusado"),
    EXPIRED("Expirado"),
}

data class Referral(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long? = null,
    val referrerPersonId: Long? = null,
    val referrerOrganizationId: Long? = null,
    val referredPersonId: Long? = null,
    val referredPatientId: Long? = null,
    val reason: String = "",
    val status: ReferralStatus = ReferralStatus.PENDING,
    val notes: String = "",
    val referredAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

// ═════════════════════════════════════════════════════════════════════
// WORKFLOW
// ═════════════════════════════════════════════════════════════════════

enum class WorkflowTriggerType(val label: String) {
    ENCOUNTER_COMPLETED("Atendimento concluído"),
    FOLLOW_UP_DUE("Follow-up pendente"),
    LEAD_QUALIFIED("Lead qualificado"),
    PATIENT_INACTIVE("Paciente inativo"),
    TASK_COMPLETED("Tarefa concluída"),
    APPOINTMENT_CREATED("Agendamento criado"),
    REFERRAL_RECEIVED("Referência recebida"),
}

enum class WorkflowActionType(val label: String) {
    CREATE_TASK("Criar tarefa"),
    CREATE_ACTIVITY("Criar atividade"),
    UPDATE_STATUS("Atualizar status"),
    SEND_NOTIFICATION("Enviar notificação"),
    ADD_TAG("Adicionar tag"),
    REMOVE_TAG("Remover tag"),
}

data class WorkflowCondition(
    val field: String,
    val operator: String,
    val value: String,
)

data class WorkflowAction(
    val type: WorkflowActionType,
    val params: Map<String, String> = emptyMap(),
)

data class CrmWorkflow(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val description: String = "",
    val triggerType: WorkflowTriggerType,
    val conditions: List<WorkflowCondition> = emptyList(),
    val actions: List<WorkflowAction> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

// ═════════════════════════════════════════════════════════════════════
// COMMUNICATION
// ═════════════════════════════════════════════════════════════════════

enum class CommunicationChannel(val label: String) {
    EMAIL("E-mail"),
    PHONE("Telefone"),
    SMS("SMS"),
    WHATSAPP("WhatsApp"),
    SYSTEM("Sistema"),
}

data class Communication(
    val id: Long = 0,
    val tenantId: Long,
    val channel: CommunicationChannel,
    val subject: String = "",
    val body: String = "",
    val from: String = "",
    val to: String = "",
    val relationType: TaskRelation = TaskRelation.GENERAL,
    val relatedEntityId: Long? = null,
    val status: String = "SENT",
    val sentAt: String? = null,
    val createdAt: String = "",
    val deletedAt: String? = null,
)

// ═════════════════════════════════════════════════════════════════════
// SAVED VIEW + FILTER
// ═════════════════════════════════════════════════════════════════════

enum class CrmViewType(val label: String) {
    TABLE("Tabela"),
    LIST("Lista"),
    KANBAN("Kanban"),
    TIMELINE("Linha do tempo"),
    CALENDAR("Calendário"),
}

data class CrmFilter(
    val field: String,
    val operator: String,
    val value: String,
)

data class SavedView(
    val id: Long = 0,
    val tenantId: Long,
    val ownerId: String = "",
    val name: String,
    val objectType: String,
    val viewType: CrmViewType = CrmViewType.LIST,
    val filters: List<CrmFilter> = emptyList(),
    val sortField: String = "",
    val sortAsc: Boolean = true,
    val columns: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

// ═════════════════════════════════════════════════════════════════════
// AUDIT TRAIL
// ═════════════════════════════════════════════════════════════════════

enum class CrmAuditEventType(val label: String) {
    PATIENT_VIEWED("Paciente visualizado"),
    CRM_RECORD_CREATED("Registro CRM criado"),
    CRM_RECORD_UPDATED("Registro CRM atualizado"),
    CRM_RECORD_DELETED("Registro CRM deletado"),
    LEAD_CONVERTED("Lead convertido"),
    OPPORTUNITY_CHANGED("Oportunidade alterada"),
    PIPELINE_CHANGED("Pipeline alterado"),
    TASK_CREATED("Tarefa criada"),
    TASK_COMPLETED("Tarefa concluída"),
    ACTIVITY_CREATED("Atividade criada"),
    REFERRAL_CREATED("Referência criada"),
    CRM_NOTE_CREATED("Nota CRM criada"),
    WORKFLOW_EXECUTED("Workflow executado"),
    VIEW_CREATED("View criada"),
    EXPORT_SOLICITADO("Exportação solicitada"),
    SEARCH_REALIZADO("Busca realizada"),
}

data class CrmAuditEvent(
    val id: Long = 0,
    val tenantId: Long,
    val eventType: CrmAuditEventType,
    val userId: String = "",
    val entityType: String = "",
    val entityId: Long? = null,
    val details: String = "",
    val timestamp: String = "",
)

// ═════════════════════════════════════════════════════════════════════
// PERMISSIONS / ROLES
// ═════════════════════════════════════════════════════════════════════

enum class CrmRole(val label: String) {
    OWNER("Proprietário"),
    ADMIN("Administrador"),
    PRACTITIONER("Profissional"),
    ASSISTANT("Assistente"),
    RECEPTION("Recepção"),
    RESEARCHER("Pesquisador"),
    BILLING("Financeiro"),
    READ_ONLY("Somente leitura"),
}

enum class CrmPermission(val label: String) {
    VIEW_PATIENT("Visualizar paciente"),
    EDIT_PATIENT("Editar paciente"),
    DELETE_PATIENT("Deletar paciente"),
    VIEW_CRM("Visualizar CRM"),
    EDIT_CRM("Editar CRM"),
    DELETE_CRM("Deletar CRM"),
    MANAGE_PIPELINES("Gerenciar pipelines"),
    MANAGE_WORKFLOWS("Gerenciar workflows"),
    MANAGE_USERS("Gerenciar usuários"),
    EXPORT_DATA("Exportar dados"),
    VIEW_TIMELINE("Visualizar timeline"),
    CREATE_TASK("Criar tarefa"),
    COMPLETE_TASK("Concluir tarefa"),
    VIEW_SEARCH("Visualizar busca"),
    MANAGE_VIEWS("Gerenciar views"),
}

data class CrmRolePermission(
    val role: CrmRole,
    val permissions: Set<CrmPermission> = emptySet(),
)

// ═════════════════════════════════════════════════════════════════════
// CARE JOURNEY
// ═════════════════════════════════════════════════════════════════════

enum class CareJourneyStage(val label: String, val order: Int) {
    LEAD("Lead", 0),
    PATIENT("Paciente", 1),
    FIRST_APPOINTMENT("Primeira consulta", 2),
    ASSESSMENT("Avaliação", 3),
    TREATMENT("Tratamento", 4),
    FOLLOW_UP("Retorno", 5),
    LONGITUDINAL_CARE("Cuidado longitudinal", 6),
}

data class CareJourney(
    val patientId: Long,
    val tenantId: Long,
    val currentStage: CareJourneyStage,
    val stagesCompleted: List<CareJourneyStage> = emptyList(),
    val firstContactAt: String? = null,
    val firstAppointmentAt: String? = null,
    val lastEncounterAt: String? = null,
    val totalEncounters: Int = 0,
    val isInTreatment: Boolean = false,
)

// ═════════════════════════════════════════════════════════════════════
// INACTIVE PATIENT ENGINE
// ═════════════════════════════════════════════════════════════════════

enum class InactiveReason(val label: String) {
    NO_ENCOUNTERS("Sem atendimentos"),
    NO_RECENT_ENCOUNTER("Sem atendimento recente"),
    NO_APPOINTMENT("Sem agendamento"),
    NO_ACTIVITY("Sem atividade"),
    FOLLOW_UP_OVERDOWN("Follow-up atrasado"),
}

data class PatientOperationalAssessment(
    val patientId: Long,
    val tenantId: Long,
    val status: PatientOperationalStatus,
    val reason: InactiveReason? = null,
    val lastEncounterDate: String? = null,
    val lastAppointmentDate: String? = null,
    val lastFollowUpDate: String? = null,
    val lastActivityDate: String? = null,
    val daysSinceLastEncounter: Int? = null,
    val daysSinceLastAppointment: Int? = null,
    val overdueFollowUps: Int = 0,
)

// ═════════════════════════════════════════════════════════════════════
// UNIFIED TIMELINE EVENT (CRM + Clinical)
// ═════════════════════════════════════════════════════════════════════

enum class UnifiedTimelineSource(val label: String) {
    CLINICAL("Clínico"),
    CRM("CRM"),
    COMMUNICATION("Comunicação"),
    ADMINISTRATIVE("Administrativo"),
    SYSTEM("Sistema"),
}

data class UnifiedTimelineEvent(
    val id: String,
    val patientId: Long? = null,
    val tenantId: Long,
    val source: UnifiedTimelineSource,
    val type: String,
    val title: String,
    val summary: String = "",
    val entityId: Long? = null,
    val entityType: String = "",
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap(),
)
