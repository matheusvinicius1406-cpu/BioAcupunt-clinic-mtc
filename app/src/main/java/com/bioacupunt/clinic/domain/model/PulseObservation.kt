package com.bioacupunt.clinic.domain.model

/**
 * Pulse observation status lifecycle:
 * CAPTURED → DRAFT → REVIEWED → CONFIRMED / REJECTED
 *
 * Manual input is always CAPTURED → DRAFT.
 * Device/AI input may start at CAPTURED → DRAFT after processing.
 */
enum class PulseObservationStatus {
    CAPTURED,
    DRAFT,
    REVIEWED,
    CONFIRMED,
    REJECTED,
}

/**
 * How the pulse data was captured.
 */
enum class PulseInputProvider {
    MANUAL,
    DEVICE,
    IMPORTED,
    AI_ASSISTED,
}

/**
 * Pulse position on the wrist (MTC convention).
 */
enum class PulsePosition(val label: String) {
    CUN("Cun (proximal)"),
    GUAN("Guan (middle)"),
    CHI("Chi (distal)"),
}

/**
 * A measured pulse feature with provenance.
 */
data class PulseFeature(
    val name: String,
    val value: String,
    val unit: String = "",
    val confidence: Double? = null,
    val source: PulseInputProvider = PulseInputProvider.MANUAL,
)

/**
 * A structured pulse observation with lifecycle tracking.
 *
 * MTC pulse positions: CUN (寸), GUAN (关), CHI (尺)
 * for both LEFT and RIGHT wrists.
 *
 * Links to StructuredObservation (type=PULSE) for clinical records.
 */
data class PulseObservation(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long? = null,
    /** Link to StructuredObservation (type=PULSE) for clinical record */
    val observationId: Long? = null,

    // --- Pulse measurements ---
    /** Depth: deep or superficial (浮/沉) */
    val depth: String = "",
    /** Rate: beats per minute */
    val rate: Int? = null,
    /** Strength: strong or weak (有力/无力) */
    val strength: String = "",
    /** Width: thin or broad (细/洪) */
    val width: String = "",
    /** Overall quality description */
    val quality: String = "",
    /** Additional quality notes */
    val qualityNotes: String = "",

    // --- Position-specific findings ---
    val leftCun: String = "",
    val leftGuan: String = "",
    val leftChi: String = "",
    val rightCun: String = "",
    val rightGuan: String = "",
    val rightChi: String = "",

    // --- Detailed features (if available from device/AI) ---
    val features: List<PulseFeature> = emptyList(),

    // --- Lifecycle ---
    val status: PulseObservationStatus = PulseObservationStatus.DRAFT,
    val source: PulseInputProvider = PulseInputProvider.MANUAL,

    // --- Review ---
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val confirmedBy: String? = null,
    val confirmedAt: String? = null,

    // --- Timestamps ---
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)
