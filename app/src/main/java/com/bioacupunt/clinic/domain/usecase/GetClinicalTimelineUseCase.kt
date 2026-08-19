package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalNote
import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.model.TimelineEventType
import com.bioacupunt.clinic.domain.model.TreatmentPlan

/**
 * Build a clinical timeline from all available clinical data.
 *
 * Aggregates encounters, observations, assessments, treatments, notes,
 * and follow-ups into a unified chronological view.
 *
 * Ordering is deterministic — same input → same timeline, always.
 */
class GetClinicalTimelineUseCase {

    fun build(
        encounters: List<Encounter>,
        observations: List<StructuredObservation>,
        notes: List<ClinicalNote>,
        treatmentPlans: List<TreatmentPlan>,
        followUps: List<FollowUp>,
    ): List<ClinicalTimelineEvent> {
        val events = mutableListOf<ClinicalTimelineEvent>()

        // Encounters
        for (encounter in encounters) {
            events.add(ClinicalTimelineEvent(
                id = "encounter-${encounter.id}",
                patientId = encounter.patientId,
                tenantId = encounter.tenantId,
                type = TimelineEventType.ENCOUNTER,
                date = encounter.startedAt.ifBlank { encounter.createdAt },
                title = encounter.type.label,
                summary = encounter.reason.ifBlank { "Atendimento" },
                entityId = encounter.id,
            ))
        }

        // Observations
        for (obs in observations) {
            events.add(ClinicalTimelineEvent(
                id = "obs-${obs.id}",
                patientId = obs.patientId,
                tenantId = obs.tenantId,
                type = TimelineEventType.OBSERVATION,
                date = obs.createdAt,
                title = obs.type.label,
                summary = obs.content.take(100),
                entityId = obs.id,
                metadata = mapOf("status" to obs.status.name, "source" to obs.source.name),
            ))
        }

        // Notes
        for (note in notes) {
            events.add(ClinicalTimelineEvent(
                id = "note-${note.id}",
                patientId = note.patientId,
                tenantId = note.tenantId,
                type = TimelineEventType.NOTE,
                date = note.createdAt,
                title = note.format.label,
                summary = note.assessment.take(100),
                entityId = note.id,
                metadata = mapOf("status" to note.status.name),
            ))
        }

        // Treatment Plans
        for (plan in treatmentPlans) {
            events.add(ClinicalTimelineEvent(
                id = "plan-${plan.id}",
                patientId = plan.patientId,
                tenantId = plan.tenantId,
                type = TimelineEventType.TREATMENT,
                date = plan.createdAt,
                title = "Plano Terapêutico",
                summary = plan.goals.take(100),
                entityId = plan.id,
                metadata = mapOf("status" to plan.status.name),
            ))
        }

        // Follow-ups
        for (fu in followUps) {
            events.add(ClinicalTimelineEvent(
                id = "fu-${fu.id}",
                patientId = fu.patientId,
                tenantId = fu.tenantId,
                type = TimelineEventType.FOLLOW_UP,
                date = fu.scheduledAt.ifBlank { fu.createdAt },
                title = "Retorno",
                summary = fu.reason.take(100),
                entityId = fu.id,
                metadata = mapOf("status" to fu.status.name),
            ))
        }

        // Sort by date descending (most recent first)
        return events.sortedByDescending { it.date }
    }
}
