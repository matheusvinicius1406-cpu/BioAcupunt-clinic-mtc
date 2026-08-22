package com.bioacupunt.mtc.knowledge.domain

import kotlinx.serialization.Serializable

// ── Pack Status ──────────────────────────────────────────────────────────────

/**
 * Installation lifecycle of a knowledge pack.
 *
 * DOWNLOADED → VALIDATING → VALID → STAGED → ACTIVE
 *                                    ↓
 *                               INVALID / FAILED
 *
 * Rollback: ACTIVE → ROLLBACK → previous ACTIVE
 */
enum class PackStatus {
    DOWNLOADED,
    VALIDATING,
    INVALID,
    VALID,
    STAGED,
    ACTIVE,
    INACTIVE,
    FAILED,
    ROLLBACK,
}

// ── Editorial Status ─────────────────────────────────────────────────────────

/**
 * Editorial lifecycle of pack content — separate from installation status.
 */
enum class EditorialStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    PUBLISHED,
    DEPRECATED,
    RETIRED,
}

// ── Pack Manifest ────────────────────────────────────────────────────────────

/**
 * Manifest describing a knowledge pack.
 *
 * The manifest is the entry point for validation, installation, and rollback.
 * It contains metadata, content counts, checksums, and version constraints.
 */
@Serializable
data class KnowledgePackManifest(
    val packId: String,
    val version: String,
    val schemaVersion: String = "1.0.0",
    val createdAt: String = "",
    val updatedAt: String = "",
    val publisher: String = "",
    val status: EditorialStatus = EditorialStatus.DRAFT,
    val contentCounts: PackContentCounts = PackContentCounts(),
    val checksum: String = "",
    val signature: String? = null,
    val minimumAppVersion: String? = null,
    val minimumSchemaVersion: String? = null,
    val previousVersion: String? = null,
    val dependencies: List<String> = emptyList(),
    val locales: List<String> = emptyList(),
    val description: String = "",
)

/**
 * Counts of each content type in the pack.
 */
@Serializable
data class PackContentCounts(
    val entities: Int = 0,
    val relations: Int = 0,
    val evidence: Int = 0,
    val sources: Int = 0,
    val citations: Int = 0,
    val assets: Int = 0,
)

// ── Knowledge Pack ───────────────────────────────────────────────────────────

/**
 * A knowledge pack containing entities, relations, evidence, sources, citations, and assets.
 *
 * Packs are the distribution unit for knowledge updates. They support:
 * - Versioning (packId + version)
 * - Validation (checksum + signature)
 * - Atomic installation (staging → activation)
 * - Rollback (to previous valid version)
 * - Diff (compare versions)
 * - Provenance tracking
 */
data class KnowledgePack(
    val manifest: KnowledgePackManifest,
    val entities: List<KnowledgeEntity> = emptyList(),
    val relations: List<KnowledgeRelation> = emptyList(),
    val evidence: List<KnowledgeEvidence> = emptyList(),
    val sources: List<KnowledgeSource> = emptyList(),
    val citations: List<KnowledgeCitation> = emptyList(),
    val assets: List<PackAsset> = emptyList(),
)

/**
 * An asset bundled with a knowledge pack (images, models, translations, etc.).
 */
data class PackAsset(
    val id: String,
    val type: String,
    val uri: String,
    val hash: String,
    val mimeType: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

// ── Pack Validation ──────────────────────────────────────────────────────────

/**
 * Result of pack validation.
 */
data class PackValidationResult(
    val isValid: Boolean,
    val errors: List<PackValidationError> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/**
 * A specific validation error.
 */
data class PackValidationError(
    val code: PackValidationErrorType,
    val message: String,
    val details: String? = null,
)

/**
 * Types of validation errors.
 */
enum class PackValidationErrorType {
    INVALID_MANIFEST,
    INVALID_SCHEMA,
    MISSING_REQUIRED_FIELD,
    INVALID_CHECKSUM,
    INVALID_SIGNATURE,
    MISSING_ENTITY,
    INVALID_RELATION,
    BROKEN_CITATION,
    BROKEN_EVIDENCE,
    DUPLICATE_ENTITY,
    VERSION_MISMATCH,
    INCOMPATIBLE_VERSION,
    APP_INCOMPATIBLE,
    SCHEMA_INCOMPATIBLE,
    CORRUPT_ARCHIVE,
    PATH_TRAVERSAL,
    OVERSIZED_PACK,
}

// ── Pack Diff ────────────────────────────────────────────────────────────────

/**
 * Result of comparing two pack versions.
 */
data class PackDiff(
    val oldVersion: String,
    val newVersion: String,
    val added: List<PackDiffItem> = emptyList(),
    val removed: List<PackDiffItem> = emptyList(),
    val changed: List<PackDiffItem> = emptyList(),
    val deprecated: List<PackDiffItem> = emptyList(),
)

/**
 * A single item in a pack diff.
 */
data class PackDiffItem(
    val entityId: String,
    val entityType: KnowledgeEntityType,
    val changeType: PackChangeType,
    val oldVersion: String? = null,
    val newVersion: String? = null,
)

/**
 * Types of changes in a pack diff.
 */
enum class PackChangeType {
    ADDED,
    REMOVED,
    CHANGED,
    DEPRECATED,
}

// ── Pack Audit Event ─────────────────────────────────────────────────────────

/**
 * An audit event for pack operations.
 */
data class PackAuditEvent(
    val timestamp: String,
    val event: PackAuditEventType,
    val packId: String,
    val version: String,
    val details: String? = null,
    val userId: String? = null,
)

/**
 * Types of pack audit events.
 */
enum class PackAuditEventType {
    DOWNLOADED,
    VALIDATED,
    STAGED,
    ACTIVATED,
    ROLLED_BACK,
    FAILED,
    DEACTIVATED,
}

// ── Installed Pack ───────────────────────────────────────────────────────────

/**
 * Represents a currently or previously installed pack.
 */
data class InstalledPack(
    val id: Long = 0,
    val tenantId: Long,
    val packId: String,
    val version: String,
    val status: PackStatus,
    val manifestJson: String = "",
    val checksum: String = "",
    val installedAt: String = "",
    val activatedAt: String? = null,
    val deactivatedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)
