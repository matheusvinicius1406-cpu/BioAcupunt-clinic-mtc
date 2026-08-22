package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.Patient360
import com.bioacupunt.crm.domain.model.PatientOperationalStatus

/**
 * Builds a comprehensive Patient 360 context for the Copilot.
 *
 * Combines clinical data (encounters, observations, treatments, follow-ups)
 * with CRM data (tasks, activities, pipeline status) into a structured context.
 *
 * The Copilot receives this context and uses it to answer questions like:
 * - "Resuma este paciente"
 * - "Qual foi o último atendimento?"
 * - "Quais tarefas estão pendentes?"
 * - "O que mudou desde a última sessão?"
 *
 * IMPORTANT: This context does NOT send the entire prontuário.
 * It sends only what's relevant for the current clinical decision.
 */
class Patient360ContextBuilder {

    /**
     * Build a complete Patient 360 view from clinical + CRM data.
     */
    fun build(
        patientId: Long,
        tenantId: Long,
        patientName: String = "",
        phone: String = "",
        email: String = "",
        encounters: List<ClinicalTimelineEvent> = emptyList(),
        observations: List<StructuredObservation> = emptyList(),
        followUps: List<FollowUp> = emptyList(),
        tasks: List<CrmTask> = emptyList(),
        activities: List<CrmActivity> = emptyList(),
        tags: List<String> = emptyList(),
        referralSource: String = "",
        currentAssessment: String = "",
        chiefComplaint: String = "",
    ): Patient360 {
        val now = System.currentTimeMillis()
        val lastEncounter = encounters.maxByOrNull { it.date }
        val pendingTasks = tasks.count { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
        val overdueTasks = tasks.count {
            it.status != TaskStatus.COMPLETED &&
            it.status != TaskStatus.CANCELLED &&
            it.dueDate.isNotEmpty() && it.dueDate < now.toString()
        }
        val lastActivity = activities.maxByOrNull { it.timestamp }

        return Patient360(
            patientId = patientId,
            tenantId = tenantId,
            name = patientName,
            phone = phone,
            email = email,
            operationalStatus = calculateOperationalStatus(encounters, followUps, activities),
            sessionCount = encounters.size,
            lastEncounterDate = lastEncounter?.date,
            currentAssessment = currentAssessment,
            chiefComplaint = chiefComplaint,
            totalActivities = activities.size,
            pendingTasks = pendingTasks,
            overdueTasks = overdueTasks,
            lastActivityDate = lastActivity?.timestamp,
            tags = tags,
            referralSource = referralSource,
            longitudinalSummary = buildLongitudinalSummary(encounters, observations, followUps),
            missingData = detectMissingData(observations, followUps, encounters),
        )
    }

    /**
     * Build a Copilot-ready context string.
     * Only includes confirmed/relevant information.
     */
    fun buildCopilotContext(patient360: Patient360): String = buildString {
        appendLine("=== CONTEXTO DO PACIENTE ===")
        appendLine("Paciente: ${patient360.name}")
        appendLine("Status operacional: ${patient360.operationalStatus.label}")
        appendLine("Total de sessões: ${patient360.sessionCount}")

        patient360.lastEncounterDate?.let {
            appendLine("Último atendimento: $it")
        }

        if (patient360.pendingTasks > 0) {
            appendLine("Tarefas pendentes: ${patient360.pendingTasks}")
        }
        if (patient360.overdueTasks > 0) {
            appendLine("⚠️ Tarefas atrasadas: ${patient360.overdueTasks}")
        }

        if (patient360.currentAssessment.isNotEmpty()) {
            appendLine("Avaliação atual: ${patient360.currentAssessment}")
        }

        if (patient360.chiefComplaint.isNotEmpty()) {
            appendLine("Queixa principal: ${patient360.chiefComplaint}")
        }

        if (patient360.missingData.isNotEmpty()) {
            appendLine("Dados faltando: ${patient360.missingData.joinToString(", ")}")
        }

        if (patient360.longitudinalSummary.isNotEmpty()) {
            appendLine("Resumo longitudinal: ${patient360.longitudinalSummary}")
        }

        if (patient360.tags.isNotEmpty()) {
            appendLine("Tags: ${patient360.tags.joinToString(", ")}")
        }

        appendLine("===========================")
    }

    private fun calculateOperationalStatus(
        encounters: List<ClinicalTimelineEvent>,
        followUps: List<FollowUp>,
        activities: List<CrmActivity>,
    ): PatientOperationalStatus {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000

        val lastEncounter = encounters.maxByOrNull { it.date }
        val lastActivity = activities.maxByOrNull { it.timestamp }

        val lastEventTime = listOfNotNull(
            lastEncounter?.date?.toLongOrNull(),
            lastActivity?.timestamp?.toLongOrNull(),
        ).maxOrNull() ?: 0L

        val daysSince = if (lastEventTime > 0) (now - lastEventTime) / (24 * 60 * 60 * 1000) else Long.MAX_VALUE

        return when {
            daysSince < 30 -> PatientOperationalStatus.ACTIVE
            daysSince < 60 -> PatientOperationalStatus.AT_RISK
            else -> PatientOperationalStatus.INACTIVE
        }
    }

    private fun buildLongitudinalSummary(
        encounters: List<ClinicalTimelineEvent>,
        observations: List<StructuredObservation>,
        followUps: List<FollowUp>,
    ): String = buildString {
        if (encounters.isNotEmpty()) {
            append("Sessões: ${encounters.size} ")
        }
        if (observations.isNotEmpty()) {
            append("Observações: ${observations.size} ")
        }
        if (followUps.isNotEmpty()) {
            append("Follow-ups: ${followUps.size}")
        }
    }

    private fun detectMissingData(
        observations: List<StructuredObservation>,
        followUps: List<FollowUp>,
        encounters: List<ClinicalTimelineEvent>,
    ): List<String> = buildList {
        if (observations.isEmpty()) add("Observações clínicas")
        if (followUps.isEmpty()) add("Follow-ups")
        if (encounters.size < 2) add("Múltiplas sessões para comparação")
    }
}
