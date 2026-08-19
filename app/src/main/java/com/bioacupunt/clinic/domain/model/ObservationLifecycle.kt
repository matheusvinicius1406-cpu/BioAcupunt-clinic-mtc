package com.bioacupunt.clinic.domain.model

/**
 * Observation status lifecycle:
 * DRAFT → REVIEWED → CONFIRMED / REJECTED
 *
 * Critical rule: AI_EXTRACTED_DRAFT ≠ CONFIRMED
 * Never auto-convert AI inference into confirmed clinical data.
 */
enum class ObservationStatus {
    DRAFT,
    REVIEWED,
    CONFIRMED,
    REJECTED,
}

/**
 * Where the observation came from.
 */
enum class ObservationSource {
    PATIENT_REPORTED,
    PRACTITIONER_OBSERVED,
    MANUAL_ENTRY,
    IMPORTED,
    VOICE_TRANSCRIPT,
    AI_EXTRACTED_DRAFT,
    REFERENCE,
}

/**
 * A structured clinical observation with lifecycle tracking.
 *
 * Extends the Phase 3 ClinicalObservation with:
 * - Status lifecycle (DRAFT → CONFIRMED)
 * - Source tracking (who/what created this)
 * - Encounter linkage
 * - Timestamps for audit
 */
data class StructuredObservation(
    val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val type: ObservationType,
    val content: String,
    val structuredData: Map<String, String> = emptyMap(),
    val status: ObservationStatus = ObservationStatus.DRAFT,
    val source: ObservationSource = ObservationSource.MANUAL_ENTRY,
    /** Source span in original text (for NLP extractions) */
    val sourceSpan: String? = null,
    /** Confidence of extraction (for AI/NLP extractions) */
    val confidence: Double? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val confirmedBy: String? = null,
    val confirmedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

enum class ObservationType(val label: String) {
    SYMPTOM("Sintoma"),
    SIGN("Sinal"),
    TONGUE("Língua"),
    PULSE("Pulso"),
    PAIN("Dor"),
    SLEEP("Sono"),
    DIGESTIVE("Digestório"),
    MENSTRUAL("Menstrual"),
    EMOTIONAL("Emocional"),
    LIFESTYLE("Estilo de vida"),
    ETIOLOGY("Etiologia"),
    HISTORY("História"),
    GENERAL("Geral"),
}
