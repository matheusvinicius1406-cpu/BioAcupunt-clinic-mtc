package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.ClinicalNoteDao
import com.bioacupunt.clinic.data.local.EncounterDao
import com.bioacupunt.clinic.data.local.FollowUpDao
import com.bioacupunt.clinic.data.local.StructuredObservationDao
import com.bioacupunt.clinic.data.local.TreatmentPlanDao
import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.TimelineEventType
import com.bioacupunt.clinic.domain.repository.ClinicalTimelineRepository

/**
 * Aggregates clinical entities into a unified timeline view.
 * Each entity type becomes a TimelineEvent with consistent structure.
 */
class ClinicalTimelineRepositoryImpl(
    private val encounterDao: EncounterDao,
    private val noteDao: ClinicalNoteDao,
    private val observationDao: StructuredObservationDao,
    private val treatmentDao: TreatmentPlanDao,
    private val followUpDao: FollowUpDao,
    private val tenantId: () -> Long,
) : ClinicalTimelineRepository {

    override suspend fun getTimeline(patientId: Long): List<ClinicalTimelineEvent> {
        val events = mutableListOf<ClinicalTimelineEvent>()

        // Encounters
        events.addAll(encounterDao.getByPatientId(patientId).map { entity ->
            ClinicalTimelineEvent(
                id = "encounter_${entity.id}",
                patientId = patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.ENCOUNTER,
                date = entity.startedAt,
                title = "Atendimento — ${entity.type}",
                summary = entity.reason.ifBlank { "Sem motivo registrado" },
                entityId = entity.id,
                metadata = mapOf("status" to entity.status),
            )
        })

        // Observations
        events.addAll(observationDao.getByPatientId(patientId, limit = 100).map { entity ->
            ClinicalTimelineEvent(
                id = "obs_${entity.id}",
                patientId = patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.OBSERVATION,
                date = entity.createdAt,
                title = entity.type,
                summary = entity.content.take(120),
                entityId = entity.id,
                metadata = mapOf("status" to entity.status, "source" to entity.source),
            )
        })

        // Notes
        events.addAll(noteDao.getByPatientId(patientId).map { entity ->
            ClinicalTimelineEvent(
                id = "note_${entity.id}",
                patientId = patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.NOTE,
                date = entity.createdAt,
                title = "Nota — ${entity.format}",
                summary = entity.assessment.take(120),
                entityId = entity.id,
                metadata = mapOf("status" to entity.status),
            )
        })

        // Treatment Plans
        events.addAll(treatmentDao.getByPatientId(patientId).map { entity ->
            ClinicalTimelineEvent(
                id = "treatment_${entity.id}",
                patientId = patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.TREATMENT,
                date = entity.createdAt,
                title = "Plano de Tratamento",
                summary = entity.goals.take(120),
                entityId = entity.id,
                metadata = mapOf("status" to entity.status),
            )
        })

        // Follow-ups
        events.addAll(followUpDao.getByPatientId(patientId).map { entity ->
            ClinicalTimelineEvent(
                id = "followup_${entity.id}",
                patientId = patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.FOLLOW_UP,
                date = entity.scheduledAt,
                title = "Retorno — ${entity.reason}",
                summary = entity.expectedFindings.take(120),
                entityId = entity.id,
                metadata = mapOf("status" to entity.status),
            )
        })

        // Sort by date descending (most recent first)
        return events.sortedByDescending { it.date }
    }

    override suspend fun getTimelineByType(patientId: Long, type: TimelineEventType): List<ClinicalTimelineEvent> {
        return getTimeline(patientId).filter { it.type == type }
    }

    override suspend fun getTimelineByDateRange(patientId: Long, from: String, to: String): List<ClinicalTimelineEvent> {
        return getTimeline(patientId).filter { it.date in from..to }
    }

    override suspend fun getRecentEvents(patientId: Long, limit: Int): List<ClinicalTimelineEvent> {
        return getTimeline(patientId).take(limit)
    }

    override suspend fun getEventsByEncounter(encounterId: Long): List<ClinicalTimelineEvent> {
        val events = mutableListOf<ClinicalTimelineEvent>()

        val encounter = encounterDao.getById(encounterId)
        if (encounter != null) {
            events.add(ClinicalTimelineEvent(
                id = "encounter_${encounter.id}",
                patientId = encounter.patientId,
                tenantId = encounter.tenantId,
                type = TimelineEventType.ENCOUNTER,
                date = encounter.startedAt,
                title = "Atendimento — ${encounter.type}",
                summary = encounter.reason.ifBlank { "Sem motivo registrado" },
                entityId = encounter.id,
                metadata = mapOf("status" to encounter.status),
            ))
        }

        events.addAll(observationDao.getByEncounterId(encounterId).map { entity ->
            ClinicalTimelineEvent(
                id = "obs_${entity.id}",
                patientId = entity.patientId,
                tenantId = entity.tenantId,
                type = TimelineEventType.OBSERVATION,
                date = entity.createdAt,
                title = entity.type,
                summary = entity.content.take(120),
                entityId = entity.id,
                metadata = mapOf("status" to entity.status, "source" to entity.source),
            )
        })

        val note = noteDao.getByEncounterId(encounterId)
        if (note != null) {
            events.add(ClinicalTimelineEvent(
                id = "note_${note.id}",
                patientId = note.patientId,
                tenantId = note.tenantId,
                type = TimelineEventType.NOTE,
                date = note.createdAt,
                title = "Nota — ${note.format}",
                summary = note.assessment.take(120),
                entityId = note.id,
                metadata = mapOf("status" to note.status),
            ))
        }

        val treatment = treatmentDao.getByEncounterId(encounterId)
        if (treatment != null) {
            events.add(ClinicalTimelineEvent(
                id = "treatment_${treatment.id}",
                patientId = treatment.patientId,
                tenantId = treatment.tenantId,
                type = TimelineEventType.TREATMENT,
                date = treatment.createdAt,
                title = "Plano de Tratamento",
                summary = treatment.goals.take(120),
                entityId = treatment.id,
                metadata = mapOf("status" to treatment.status),
            ))
        }

        return events.sortedByDescending { it.date }
    }
}
