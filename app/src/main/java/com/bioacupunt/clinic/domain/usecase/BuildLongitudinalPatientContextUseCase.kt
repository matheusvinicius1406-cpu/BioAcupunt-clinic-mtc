package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.FollowUpStatus
import com.bioacupunt.clinic.domain.model.LongitudinalPatientContext
import com.bioacupunt.clinic.domain.repository.EncounterRepository
import com.bioacupunt.clinic.domain.repository.FollowUpRepository
import com.bioacupunt.clinic.domain.repository.ObservationRepository

/**
 * Builds a LongitudinalPatientContext — a focused, relevant subset of a patient's
 * clinical history for the copilot and clinical intelligence.
 *
 * NOT the full prontuário. Only information relevant to the current clinical decision:
 * - recent observations (last 20)
 * - persistent/recurring patterns
 * - recent assessments (titles only)
 * - treatment history (titles only)
 * - upcoming follow-ups
 * - current concerns
 */
class BuildLongitudinalPatientContextUseCase(
    private val encounterRepository: EncounterRepository,
    private val observationRepository: ObservationRepository,
    private val followUpRepository: FollowUpRepository,
) {

    suspend fun build(patientId: Long): LongitudinalPatientContext {
        val encounters = encounterRepository.getRecent(patientId, limit = 10)
        val observations = observationRepository.getByPatientId(patientId, limit = 20)
        val followUps = followUpRepository.getByPatientIdAndStatus(patientId, FollowUpStatus.SCHEDULED)

        val persistentFindings = extractPersistentFindings(observations)
        val recurringPatterns = extractRecurringPatterns(observations)
        val currentConcerns = extractCurrentConcerns(observations)

        return LongitudinalPatientContext(
            patientId = patientId,
            recentObservations = observations,
            persistentFindings = persistentFindings,
            recentAssessments = encounters.map { encounter ->
                "Atendimento ${encounter.startedAt} — ${encounter.reason.ifBlank { "Sem motivo" }}"
            },
            treatmentHistory = emptyList(), // populated when TreatmentPlan data is available
            followUps = followUps,
            recentNotes = emptyList(), // populated when ClinicalNote data is available
            recurringPatterns = recurringPatterns,
            currentConcerns = currentConcerns,
            sessionCount = encounterRepository.countByPatientId(patientId),
            lastEncounterDate = encounters.firstOrNull()?.startedAt,
        )
    }

    private fun extractPersistentFindings(observations: List<com.bioacupunt.clinic.domain.model.StructuredObservation>): List<String> {
        // Observations that appear multiple times are "persistent"
        val grouped = observations.groupBy { it.content.lowercase().trim() }
        return grouped.filter { it.value.size >= 2 }
            .map { "${it.key} (registrado ${it.value.size} vezes)" }
    }

    private fun extractRecurringPatterns(observations: List<com.bioacupunt.clinic.domain.model.StructuredObservation>): List<String> {
        // Type-level aggregation
        val typeCounts = observations.groupBy { it.type }
            .map { "${it.key.label}: ${it.value.size} registros" }
            .sortedByDescending { it.substringAfter(": ").substringBefore(" ").toIntOrNull() ?: 0 }
        return typeCounts.take(5)
    }

    private fun extractCurrentConcerns(observations: List<com.bioacupunt.clinic.domain.model.StructuredObservation>): List<String> {
        // Most recent observations that haven't been confirmed (still draft)
        return observations
            .filter { it.status == com.bioacupunt.clinic.domain.model.ObservationStatus.DRAFT }
            .take(5)
            .map { it.content.take(100) }
    }
}
