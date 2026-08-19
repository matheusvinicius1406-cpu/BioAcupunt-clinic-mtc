package com.bioacupunt.clinic.domain.model

/**
 * A timeline event — a single point in a patient's clinical history.
 *
 * Aggregates encounters, observations, assessments, treatments, notes, and follow-ups
 * into a unified chronological view.
 */
data class ClinicalTimelineEvent(
    val id: String,
    val patientId: Long,
    val tenantId: Long,
    val type: TimelineEventType,
    val date: String,
    val title: String,
    val summary: String = "",
    /** Reference to the original entity (encounterId, noteId, etc.) */
    val entityId: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
)

enum class TimelineEventType(val label: String) {
    ENCOUNTER("Atendimento"),
    OBSERVATION("Observação"),
    ASSESSMENT("Avaliação"),
    TREATMENT("Tratamento"),
    NOTE("Nota"),
    FOLLOW_UP("Retorno"),
    DOCUMENT("Documento"),
}

/**
 * Longitudinal patient context — aggregated relevant information for the copilot.
 *
 * Contains only information relevant to the current clinical decision,
 * NOT the entire prontuário.
 */
data class LongitudinalPatientContext(
    val patientId: Long,
    val recentObservations: List<StructuredObservation> = emptyList(),
    val persistentFindings: List<String> = emptyList(),
    val recentAssessments: List<String> = emptyList(),
    val treatmentHistory: List<String> = emptyList(),
    val followUps: List<FollowUp> = emptyList(),
    val recentNotes: List<String> = emptyList(),
    val recurringPatterns: List<String> = emptyList(),
    val currentConcerns: List<String> = emptyList(),
    val sessionCount: Int = 0,
    val lastEncounterDate: String? = null,
)

/**
 * Result of comparing two clinical sessions.
 *
 * All differences are derived from structured data, never from text inference.
 */
data class SessionComparison(
    val sessionAId: Long,
    val sessionBId: Long,
    val newFindings: List<String> = emptyList(),
    val resolvedFindings: List<String> = emptyList(),
    val persistentFindings: List<String> = emptyList(),
    val worsenedFindings: List<String> = emptyList(),
    val improvedFindings: List<String> = emptyList(),
    val patternChanges: List<String> = emptyList(),
    val treatmentChanges: List<String> = emptyList(),
)
