package com.bioacupunt.crm

import com.bioacupunt.crm.domain.model.CareJourney
import com.bioacupunt.crm.domain.model.CareJourneyStage
import com.bioacupunt.crm.domain.model.Communication
import com.bioacupunt.crm.domain.model.CommunicationChannel
import com.bioacupunt.crm.domain.model.CrmAuditEvent
import com.bioacupunt.crm.domain.model.CrmAuditEventType
import com.bioacupunt.crm.domain.model.CrmFilter
import com.bioacupunt.crm.domain.model.CrmNote
import com.bioacupunt.crm.domain.model.CrmNoteType
import com.bioacupunt.crm.domain.model.CrmOpportunity
import com.bioacupunt.crm.domain.model.CrmPermission
import com.bioacupunt.crm.domain.model.CrmRole
import com.bioacupunt.crm.domain.model.CrmViewType
import com.bioacupunt.crm.domain.model.CrmWorkflow
import com.bioacupunt.crm.domain.model.InactiveReason
import com.bioacupunt.crm.domain.model.OpportunityStatus
import com.bioacupunt.crm.domain.model.OpportunityType
import com.bioacupunt.crm.domain.model.PatientOperationalAssessment
import com.bioacupunt.crm.domain.model.Referral
import com.bioacupunt.crm.domain.model.ReferralStatus
import com.bioacupunt.crm.domain.model.SavedView
import com.bioacupunt.crm.domain.model.UnifiedTimelineEvent
import com.bioacupunt.crm.domain.model.UnifiedTimelineSource
import com.bioacupunt.crm.domain.model.WorkflowAction
import com.bioacupunt.crm.domain.model.WorkflowActionType
import com.bioacupunt.crm.domain.model.WorkflowCondition
import com.bioacupunt.crm.domain.model.WorkflowTriggerType
import com.bioacupunt.crm.domain.usecase.InactivePatientEngine
import com.bioacupunt.crm.domain.usecase.Patient360ContextBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrmPhase7ModelsTest {

    // ═════════════════════════════════════════════════════════════════════
    // OPPORTUNITY
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun opportunityType_allValuesExist() {
        assertEquals(6, OpportunityType.values().size)
        assertEquals("Novo paciente", OpportunityType.NEW_PATIENT.label)
        assertEquals("Referência", OpportunityType.REFERRAL.label)
    }

    @Test
    fun opportunityStatus_allValuesExist() {
        assertEquals(4, OpportunityStatus.values().size)
        assertEquals("Aberta", OpportunityStatus.OPEN.label)
        assertEquals("Ganha", OpportunityStatus.WON.label)
    }

    @Test
    fun crmOpportunity_defaultValues() {
        val opp = CrmOpportunity(tenantId = 1L, name = "Nova paciente")
        assertEquals("Nova paciente", opp.name)
        assertEquals(OpportunityType.NEW_PATIENT, opp.type)
        assertEquals(OpportunityStatus.OPEN, opp.status)
        assertEquals(0.0, opp.value, 0.001)
        assertEquals("BRL", opp.currency)
    }

    // ═════════════════════════════════════════════════════════════════════
    // CRM NOTE
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun crmNoteType_allValuesExist() {
        assertEquals(6, CrmNoteType.values().size)
        assertEquals("Geral", CrmNoteType.GENERAL.label)
        assertEquals("Follow-up", CrmNoteType.FOLLOW_UP.label)
        assertEquals("Alerta", CrmNoteType.ALERT.label)
    }

    @Test
    fun crmNote_isNotClinicalNote() {
        val note = CrmNote(patientId = 1L, content = "Paciente pediu reagendamento", type = "operational")
        assertEquals("Paciente pediu reagendamento", note.content)
        assertEquals("operational", note.type)
    }

    // ═════════════════════════════════════════════════════════════════════
    // REFERRAL
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun referralStatus_allValuesExist() {
        assertEquals(6, ReferralStatus.values().size)
        assertEquals("Pendente", ReferralStatus.PENDING.label)
        assertEquals("Convertido", ReferralStatus.CONVERTED.label)
        assertEquals("Recusado", ReferralStatus.DECLINED.label)
    }

    @Test
    fun referral_defaultValues() {
        val ref = Referral(tenantId = 1L, reason = "Dor lombar crônica")
        assertEquals("Dor lombar crônica", ref.reason)
        assertEquals(ReferralStatus.PENDING, ref.status)
        assertNull(ref.patientId)
    }

    // ═════════════════════════════════════════════════════════════════════
    // WORKFLOW
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun workflowTriggerType_allValuesExist() {
        assertEquals(7, WorkflowTriggerType.values().size)
        assertEquals("Atendimento concluído", WorkflowTriggerType.ENCOUNTER_COMPLETED.label)
        assertEquals("Paciente inativo", WorkflowTriggerType.PATIENT_INACTIVE.label)
    }

    @Test
    fun workflowActionType_allValuesExist() {
        assertEquals(6, WorkflowActionType.values().size)
        assertEquals("Criar tarefa", WorkflowActionType.CREATE_TASK.label)
        assertEquals("Enviar notificação", WorkflowActionType.SEND_NOTIFICATION.label)
    }

    @Test
    fun workflow_conditionAndAction() {
        val workflow = CrmWorkflow(
            tenantId = 1L,
            name = "Follow-up automático",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            conditions = listOf(WorkflowCondition(field = "followUpRequired", operator = "equals", value = "true")),
            actions = listOf(WorkflowAction(type = WorkflowActionType.CREATE_TASK, params = mapOf("title" to "Follow-up pendente"))),
        )
        assertEquals(1, workflow.conditions.size)
        assertEquals(1, workflow.actions.size)
        assertTrue(workflow.isActive)
    }

    // ═════════════════════════════════════════════════════════════════════
    // COMMUNICATION
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun communicationChannel_allValuesExist() {
        assertEquals(5, CommunicationChannel.values().size)
        assertEquals("E-mail", CommunicationChannel.EMAIL.label)
        assertEquals("WhatsApp", CommunicationChannel.WHATSAPP.label)
        assertEquals("Sistema", CommunicationChannel.SYSTEM.label)
    }

    @Test
    fun communication_defaultValues() {
        val comm = Communication(tenantId = 1L, channel = CommunicationChannel.EMAIL, subject = "Lembrete")
        assertEquals("Lembrete", comm.subject)
        assertEquals("SENT", comm.status)
    }

    // ═════════════════════════════════════════════════════════════════════
    // SAVED VIEW
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun crmViewType_allValuesExist() {
        assertEquals(5, CrmViewType.values().size)
        assertEquals("Tabela", CrmViewType.TABLE.label)
        assertEquals("Kanban", CrmViewType.KANBAN.label)
        assertEquals("Calendário", CrmViewType.CALENDAR.label)
    }

    @Test
    fun savedView_withFilters() {
        val view = SavedView(
            tenantId = 1L,
            name = "Pacientes ativos",
            objectType = "PATIENT",
            viewType = CrmViewType.LIST,
            filters = listOf(CrmFilter(field = "status", operator = "equals", value = "ACTIVE")),
        )
        assertEquals("Pacientes ativos", view.name)
        assertEquals(1, view.filters.size)
        assertEquals("status", view.filters[0].field)
    }

    // ═════════════════════════════════════════════════════════════════════
    // AUDIT TRAIL
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun crmAuditEventType_allValuesExist() {
        assertEquals(16, CrmAuditEventType.values().size)
        assertEquals("Lead convertido", CrmAuditEventType.LEAD_CONVERTED.label)
        assertEquals("Exportação solicitada", CrmAuditEventType.EXPORT_SOLICITADO.label)
    }

    @Test
    fun crmAuditEvent_storesEntityReference() {
        val event = CrmAuditEvent(
            tenantId = 1L,
            eventType = CrmAuditEventType.CRM_RECORD_CREATED,
            entityType = "PERSON",
            entityId = 42L,
            timestamp = "2026-08-20T12:00:00Z",
        )
        assertEquals("PERSON", event.entityType)
        assertEquals(42L, event.entityId)
    }

    // ═════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun crmRole_allValuesExist() {
        assertEquals(8, CrmRole.values().size)
        assertEquals("Proprietário", CrmRole.OWNER.label)
        assertEquals("Profissional", CrmRole.PRACTITIONER.label)
        assertEquals("Somente leitura", CrmRole.READ_ONLY.label)
    }

    @Test
    fun crmPermission_allValuesExist() {
        assertEquals(15, CrmPermission.values().size)
        assertEquals("Visualizar paciente", CrmPermission.VIEW_PATIENT.label)
        assertEquals("Exportar dados", CrmPermission.EXPORT_DATA.label)
    }

    // ═════════════════════════════════════════════════════════════════════
    // CARE JOURNEY
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun careJourneyStage_allValuesExist() {
        assertEquals(7, CareJourneyStage.values().size)
        assertEquals("Lead", CareJourneyStage.LEAD.label)
        assertEquals("Cuidado longitudinal", CareJourneyStage.LONGITUDINAL_CARE.label)
    }

    @Test
    fun careJourney_storesJourney() {
        val journey = CareJourney(
            patientId = 10L,
            tenantId = 1L,
            currentStage = CareJourneyStage.TREATMENT,
            stagesCompleted = listOf(CareJourneyStage.LEAD, CareJourneyStage.PATIENT, CareJourneyStage.FIRST_APPOINTMENT),
            totalEncounters = 5,
            isInTreatment = true,
        )
        assertEquals(CareJourneyStage.TREATMENT, journey.currentStage)
        assertEquals(3, journey.stagesCompleted.size)
        assertTrue(journey.isInTreatment)
    }

    // ═════════════════════════════════════════════════════════════════════
    // INACTIVE REASON
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun inactiveReason_allValuesExist() {
        assertEquals(5, InactiveReason.values().size)
        assertEquals("Sem atendimentos", InactiveReason.NO_ENCOUNTERS.label)
        assertEquals("Follow-up atrasado", InactiveReason.FOLLOW_UP_OVERDOWN.label)
    }

    // ═════════════════════════════════════════════════════════════════════
    // UNIFIED TIMELINE
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun unifiedTimelineSource_allValuesExist() {
        assertEquals(5, UnifiedTimelineSource.values().size)
        assertEquals("Clínico", UnifiedTimelineSource.CLINICAL.label)
        assertEquals("CRM", UnifiedTimelineSource.CRM.label)
    }

    @Test
    fun unifiedTimelineEvent_hasDeterministicOrdering() {
        val event = UnifiedTimelineEvent(
            id = "evt-001",
            tenantId = 1L,
            source = UnifiedTimelineSource.CLINICAL,
            type = "ENCOUNTER",
            title = "Sessão 5",
            timestamp = "2026-08-20T10:00:00Z",
        )
        assertEquals("evt-001", event.id)
        assertEquals("2026-08-20T10:00:00Z", event.timestamp)
    }

    // ═════════════════════════════════════════════════════════════════════
    // INACTIVE PATIENT ENGINE
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun inactiveEngine_noEncounters_inactive() {
        val engine = InactivePatientEngine()
        val assessment = engine.assess(patientId = 1L, tenantId = 1L)
        assertEquals(com.bioacupunt.crm.domain.model.PatientOperationalStatus.INACTIVE, assessment.status)
        assertEquals(InactiveReason.NO_ENCOUNTERS, assessment.reason)
    }

    @Test
    fun inactiveEngine_assessment_hasCorrectFields() {
        val engine = InactivePatientEngine()
        val assessment = engine.assess(patientId = 10L, tenantId = 1L)
        assertEquals(10L, assessment.patientId)
        assertEquals(1L, assessment.tenantId)
        assertEquals(0, assessment.overdueFollowUps)
    }

    // ═════════════════════════════════════════════════════════════════════
    // PATIENT 360 CONTEXT BUILDER
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun contextBuilder_buildsCompleteView() {
        val builder = Patient360ContextBuilder()
        val p360 = builder.build(
            patientId = 10L,
            tenantId = 1L,
            patientName = "Maria",
        )
        assertEquals("Maria", p360.name)
        assertEquals(10L, p360.patientId)
    }

    @Test
    fun contextBuilder_copilotContext_containsEssentialInfo() {
        val builder = Patient360ContextBuilder()
        val p360 = builder.build(
            patientId = 10L,
            tenantId = 1L,
            patientName = "Maria",
            currentAssessment = "Qi e Xue Deficiência",
            chiefComplaint = "Fadiga",
        )
        val context = builder.buildCopilotContext(p360)
        assertTrue(context.contains("Maria"))
        assertTrue(context.contains("Qi e Xue Deficiência"))
        assertTrue(context.contains("Fadiga"))
        assertTrue(context.contains("CONTEXTO DO PACIENTE"))
    }

    @Test
    fun contextBuilder_detectsMissingData() {
        val builder = Patient360ContextBuilder()
        val p360 = builder.build(patientId = 10L, tenantId = 1L)
        assertTrue(p360.missingData.isNotEmpty())
    }
}
