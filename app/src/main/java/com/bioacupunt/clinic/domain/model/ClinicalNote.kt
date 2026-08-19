package com.bioacupunt.clinic.domain.model

/**
 * A structured clinical note for an encounter.
 *
 * Supports SOAP format and MTC-specific evolution notes.
 * Every note has a lifecycle: DRAFT → REVIEWED → FINAL.
 *
 * AI can generate DRAFT notes, but NEVER auto-finalize.
 * The professional must REVIEW and CONFIRM.
 */
data class ClinicalNote(
    val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val format: NoteFormat = NoteFormat.SOAP,
    /** Subjective: what the patient reports */
    val subjective: String = "",
    /** Objective: what the practitioner observes */
    val objective: String = "",
    /** Assessment: clinical impression */
    val assessment: String = "",
    /** Plan: treatment plan */
    val plan: String = "",
    /** MTC-specific assessment (can reference MtcAssessment) */
    val mtcAssessmentSummary: String = "",
    /** References to other clinical data */
    val references: List<String> = emptyList(),
    val status: NoteStatus = NoteStatus.DRAFT,
    val createdBy: String = "",
    val finalizedBy: String? = null,
    val finalizedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

enum class NoteFormat(val label: String) {
    SOAP("SOAP"),
    MTC_EVOLUTION("Evolução MTC"),
    FOLLOW_UP("Retorno"),
    DISCHARGE("Alta"),
}

enum class NoteStatus {
    DRAFT,
    REVIEWED,
    FINAL,
}
