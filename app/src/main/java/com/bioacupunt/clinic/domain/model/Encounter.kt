package com.bioacupunt.clinic.domain.model

/**
 * A clinical encounter â€” a single clinical session with a patient.
 *
 * Distinct from [com.bioacupunt.agenda.domain.model.Appointment]:
 * - Appointment = scheduling (when the session happens)
 * - Encounter = the clinical event itself (what happened)
 *
 * States:
 * - PLANNED: scheduled but not started
 * - IN_PROGRESS: actively being conducted
 * - PAUSED: temporarily suspended
 * - COMPLETED: clinical work finished
 * - CANCELLED: encounter was cancelled
 */
data class Encounter(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val status: EncounterStatus = EncounterStatus.PLANNED,
    val type: EncounterType = EncounterType.ACUPUNCTURE,
    val startedAt: String = "",
    val endedAt: String = "",
    val practitionerId: String = "",
    val reason: String = "",
    /** Link to the appointment that generated this encounter, if any. */
    val appointmentId: Long? = null,
    /** Link to the current MTC assessment for this encounter. */
    val currentAssessmentId: Long? = null,
    /** Link to the current clinical note for this encounter. */
    val currentNoteId: Long? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

enum class EncounterStatus {
    PLANNED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

enum class EncounterType(val label: String) {
    ACUPUNCTURE("Acupuntura"),
    CONSULTATION("Consulta"),
    FOLLOW_UP("Retorno"),
    ASSESSMENT("AvaliaÃ§Ã£o"),
    TREATMENT("Tratamento"),
}

