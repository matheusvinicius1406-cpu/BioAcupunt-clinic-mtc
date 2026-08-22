package com.bioacupunt.clinic.domain.model

/**
 * Clinical media types.
 */
enum class ClinicalMediaType(val label: String) {
    IMAGE("Imagem"),
    AUDIO("Áudio"),
    VIDEO("Vídeo"),
    DOCUMENT("Documento"),
}

/**
 * Media lifecycle status.
 * CAPTURED → VALIDATED → STORED → PROCESSING → PROCESSED → REVIEWED → CONFIRMED / REJECTED
 */
enum class ClinicalMediaStatus {
    CAPTURED,
    VALIDATED,
    STORED,
    PROCESSING,
    PROCESSED,
    REVIEWED,
    CONFIRMED,
    REJECTED,
    DELETED,
}

/**
 * Where the media came from.
 */
enum class ClinicalMediaSource {
    CAMERA,
    IMAGE_PICKER,
    AUDIO_RECORDER,
    VIDEO_RECORDER,
    IMPORTED,
    FHIR_IMPORT,
    SYSTEM,
    AI_PROCESSED,
}

/**
 * Clinical media record — image, audio, video, or document linked to a patient/encounter.
 *
 * Metadata lives in Room; binary content is stored in secure app-internal storage.
 * The `uri` is an app-private URI, never a public path.
 */
data class ClinicalMedia(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long? = null,
    val type: ClinicalMediaType,
    /** App-private URI — never exposed as public path */
    val uri: String,
    val mimeType: String,
    /** Original filename (if from picker/import) */
    val originalName: String? = null,
    /** File size in bytes */
    val sizeBytes: Long = 0,
    /** SHA-256 hash of the original binary content */
    val hash: String = "",
    val source: ClinicalMediaSource = ClinicalMediaSource.CAMERA,
    val status: ClinicalMediaStatus = ClinicalMediaStatus.CAPTURED,
    /** How the media is linked clinically (e.g. "tongue_photo", "consent_form") */
    val category: String = "",
    /** Free-text description */
    val description: String = "",
    /** Processing version if AI/vision was applied */
    val processingVersion: String? = null,
    /** When the media was physically captured (may differ from createdAt) */
    val capturedAt: String? = null,
    /** User/device that captured */
    val capturedBy: String? = null,
    /** Device identifier for provenance */
    val deviceInfo: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/**
 * Maximum allowed sizes by media type (configurable).
 */
object MediaSizeLimits {
    const val MAX_IMAGE_BYTES = 20L * 1024 * 1024       // 20 MB
    const val MAX_AUDIO_BYTES = 100L * 1024 * 1024      // 100 MB
    const val MAX_VIDEO_BYTES = 500L * 1024 * 1024      // 500 MB
    const val MAX_DOCUMENT_BYTES = 50L * 1024 * 1024    // 50 MB

    fun maxForType(type: ClinicalMediaType): Long = when (type) {
        ClinicalMediaType.IMAGE -> MAX_IMAGE_BYTES
        ClinicalMediaType.AUDIO -> MAX_AUDIO_BYTES
        ClinicalMediaType.VIDEO -> MAX_VIDEO_BYTES
        ClinicalMediaType.DOCUMENT -> MAX_DOCUMENT_BYTES
    }
}
