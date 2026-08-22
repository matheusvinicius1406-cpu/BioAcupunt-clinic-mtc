package com.bioacupunt.clinic.domain.model

/**
 * Tongue observation status lifecycle:
 * CAPTURED → FEATURES_EXTRACTED → DRAFT → REVIEWED → CONFIRMED / REJECTED
 *
 * Critical rule: AI_EXTRACTED_DRAFT ≠ CONFIRMED
 * Never auto-convert vision output into confirmed clinical data.
 */
enum class TongueObservationStatus {
    CAPTURED,
    FEATURES_EXTRACTED,
    DRAFT,
    REVIEWED,
    CONFIRMED,
    REJECTED,
}

/**
 * Source of the tongue observation.
 */
enum class TongueObservationSource {
    MANUAL,
    PRACTITIONER_OBSERVED,
    IMAGE,
    AI_EXTRACTED_DRAFT,
    IMPORTED,
}

/**
 * Tongue body color (MTC classification).
 */
enum class TongueBodyColor(val label: String) {
    PALE("Pálida"),
    NORMAL("Normal"),
    RED("Vermelha"),
    DARK_RED("Vermelho Escuro"),
    PURPLE("Roxa"),
    BLUE_PURPLE("Azul-Roxa"),
    CRIMSON("Carmesim"),
}

/**
 * Tongue shape.
 */
enum class TongueShape(val label: String) {
    NORMAL("Normal"),
    THIN("Fina"),
    THICK("Espessa"),
    SWOLLEN("Inchada"),
    SCALLOPED("Dentada"),
    WOODEN("Amadeirada"),
    LONG("Longa"),
    SHORT("Curta"),
}

/**
 * Tongue coating.
 */
enum class TongueCoating(val label: String) {
    NONE("Sem"),
    THIN_WHITE("Fino Branco"),
    THICK_WHITE("Espesso Branco"),
    THIN_YELLOW("Fino Amarelo"),
    THICK_YELLOW("Espesso Amarelo"),
    GRAY("Cinza"),
    BLACK("Preto"),
    PEELING ("Descascando"),
}

/**
 * Tongue moisture level.
 */
enum class TongueMoisture(val label: String) {
    DRY("Seca"),
    NORMAL("Normal"),
    MOIST("Úmida"),
    WET("Molhada"),
    SLIPPERY("Escorregadia"),
}

/**
 * A structured tongue observation with lifecycle tracking.
 *
 * Links to:
 * - ClinicalMedia (the photo, if IMAGE source)
 * - StructuredObservation (type=TONGUE for clinical records)
 * - KnowledgeCore (for interpretation rules)
 *
 * Vision output goes through AI_EXTRACTED_DRAFT → professional review → CONFIRMED.
 * The vision model NEVER auto-confirms.
 */
data class TongueObservation(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long? = null,
    /** Link to ClinicalMedia if this observation came from a photo */
    val mediaId: Long? = null,
    /** Link to StructuredObservation (type=TONGUE) for clinical record */
    val observationId: Long? = null,

    // --- Tongue features ---
    val bodyColor: TongueBodyColor? = null,
    val bodyColorNotes: String = "",
    val shape: TongueShape? = null,
    val shapeNotes: String = "",
    val coating: TongueCoating? = null,
    val coatingNotes: String = "",
    val moisture: TongueMoisture? = null,
    val moistureNotes: String = "",
    val cracks: String = "",
    val marks: String = "",
    val movement: String = "",
    val specialFindings: String = "",

    // --- Regions ---
    val regionTip: String = "",
    val regionCenter: String = "",
    val regionRoot: String = "",
    val regionLeft: String = "",
    val regionRight: String = "",

    // --- Lifecycle ---
    val status: TongueObservationStatus = TongueObservationStatus.DRAFT,
    val source: TongueObservationSource = TongueObservationSource.MANUAL,

    // --- Vision provenance (if AI was used) ---
    val visionModelName: String? = null,
    val visionModelVersion: String? = null,
    val visionConfidence: Double? = null,
    val preprocessingVersion: String? = null,

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
