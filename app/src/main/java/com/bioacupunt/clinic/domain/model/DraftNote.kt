package com.bioacupunt.clinic.domain.model

/**
 * AI-generated clinical note draft.
 *
 * NEVER auto-finalized. The professional must REVIEW → EDIT → CONFIRM → FINAL.
 *
 * Flow:
 * AI → DRAFT → PROFESSIONAL REVIEW → EDIT → CONFIRM → FINAL
 */
data class ClinicalNoteDraft(
    val encounterId: Long,
    val patientId: Long,
    val format: NoteFormat,
    val subjective: String = "",
    val objective: String = "",
    val assessment: String = "",
    val plan: String = "",
    val mtcAssessmentSummary: String = "",
    val evidenceSources: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val confidence: String = "LOW",
    val generatedAt: String = "",
)

/**
 * Professional review result for a draft note.
 */
data class NoteReviewResult(
    val noteId: Long,
    val reviewerId: String,
    val action: ReviewAction,
    val comments: String = "",
    val reviewedAt: String = "",
)

enum class ReviewAction {
    ACCEPTED,
    EDITED,
    REJECTED,
}
