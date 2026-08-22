package com.bioacupunt.mtc.knowledge.domain

/**
 * Validates a knowledge pack for integrity, schema compliance, and consistency.
 *
 * Validation steps:
 * 1. Manifest validation (required fields, format)
 * 2. Schema validation (entity types, relation types)
 * 3. Reference validation (citations → sources, evidence → citations)
 * 4. Uniqueness validation (no duplicate entity IDs)
 * 5. Checksum verification (content integrity)
 * 6. Signature verification (optional, trusted key)
 * 7. Version compatibility (app version, schema version)
 */
class KnowledgePackValidator {

    /**
     * Validate a complete knowledge pack.
     *
     * @param pack The pack to validate
     * @param currentAppVersion The current app version (for compatibility check)
     * @param currentSchemaVersion The current schema version (for compatibility check)
     * @return Validation result with errors and warnings
     */
    fun validate(
        pack: KnowledgePack,
        currentAppVersion: String = "",
        currentSchemaVersion: String = "",
    ): PackValidationResult {
        val errors = mutableListOf<PackValidationError>()
        val warnings = mutableListOf<String>()

        // 1. Manifest validation
        validateManifest(pack.manifest, errors)

        // 2. Entity validation
        validateEntities(pack.entities, errors, warnings)

        // 3. Relation validation
        validateRelations(pack.relations, pack.entities, errors, warnings)

        // 4. Reference validation
        validateReferences(pack, errors, warnings)

        // 5. Checksum validation
        if (pack.manifest.checksum.isNotBlank()) {
            validateChecksum(pack, errors)
        }

        // 6. Signature validation (optional)
        if (pack.manifest.signature != null) {
            validateSignature(pack, errors)
        }

        // 7. Version compatibility
        validateCompatibility(pack.manifest, currentAppVersion, currentSchemaVersion, errors)

        return PackValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    private fun validateManifest(manifest: KnowledgePackManifest, errors: MutableList<PackValidationError>) {
        if (manifest.packId.isBlank()) {
            errors.add(PackValidationError(PackValidationErrorType.MISSING_REQUIRED_FIELD, "packId is blank"))
        }
        if (manifest.version.isBlank()) {
            errors.add(PackValidationError(PackValidationErrorType.MISSING_REQUIRED_FIELD, "version is blank"))
        }
        if (manifest.schemaVersion.isBlank()) {
            errors.add(PackValidationError(PackValidationErrorType.MISSING_REQUIRED_FIELD, "schemaVersion is blank"))
        }
    }

    private fun validateEntities(
        entities: List<KnowledgeEntity>,
        errors: MutableList<PackValidationError>,
        warnings: MutableList<String>,
    ) {
        val seenIds = mutableSetOf<String>()
        for (entity in entities) {
            if (entity.id.isBlank()) {
                errors.add(PackValidationError(PackValidationErrorType.INVALID_SCHEMA, "Entity with blank id"))
                continue
            }
            if (entity.id in seenIds) {
                errors.add(PackValidationError(PackValidationErrorType.DUPLICATE_ENTITY, "Duplicate entity id: ${entity.id}"))
            }
            seenIds.add(entity.id)

            if (entity.canonicalName.isBlank()) {
                warnings.add("Entity ${entity.id} has blank canonicalName")
            }
        }
    }

    private fun validateRelations(
        relations: List<KnowledgeRelation>,
        entities: List<KnowledgeEntity>,
        errors: MutableList<PackValidationError>,
        warnings: MutableList<String>,
    ) {
        val entityIds = entities.map { it.id }.toSet()
        for (relation in relations) {
            if (relation.sourceEntityId.isBlank() || relation.targetEntityId.isBlank()) {
                errors.add(PackValidationError(PackValidationErrorType.INVALID_RELATION, "Relation with blank entity id"))
            }
            if (relation.sourceEntityId !in entityIds) {
                warnings.add("Relation references missing source entity: ${relation.sourceEntityId}")
            }
            if (relation.targetEntityId !in entityIds) {
                warnings.add("Relation references missing target entity: ${relation.targetEntityId}")
            }
        }
    }

    private fun validateReferences(
        pack: KnowledgePack,
        errors: MutableList<PackValidationError>,
        warnings: MutableList<String>,
    ) {
        val sourceIds = pack.sources.map { it.id }.toSet()
        val citationIds = pack.citations.map { it.id }.toSet()

        // Citations must reference valid sources
        for (citation in pack.citations) {
            if (citation.sourceId !in sourceIds) {
                errors.add(PackValidationError(PackValidationErrorType.BROKEN_CITATION, "Citation ${citation.id} references missing source: ${citation.sourceId}"))
            }
        }

        // Evidence must reference valid citations
        for (ev in pack.evidence) {
            for (citationId in ev.citationIds) {
                if (citationId !in citationIds) {
                    errors.add(PackValidationError(PackValidationErrorType.BROKEN_EVIDENCE, "Evidence ${ev.id} references missing citation: $citationId"))
                }
            }
        }
    }

    private fun validateChecksum(pack: KnowledgePack, errors: MutableList<PackValidationError>) {
        if (pack.manifest.checksum.isBlank()) {
            errors.add(PackValidationError(PackValidationErrorType.INVALID_CHECKSUM, "Checksum is blank"))
            return
        }
        if (!PackChecksum.verify(pack, pack.manifest.checksum)) {
            errors.add(PackValidationError(PackValidationErrorType.INVALID_CHECKSUM, "Checksum mismatch: expected ${pack.manifest.checksum}, computed ${PackChecksum.compute(pack)}"))
        }
    }

    private fun validateSignature(pack: KnowledgePack, errors: MutableList<PackValidationError>) {
        if (pack.manifest.signature.isNullOrBlank()) {
            errors.add(PackValidationError(PackValidationErrorType.INVALID_SIGNATURE, "Signature is present but blank"))
        }
        // Actual signature verification requires a trusted public key
        // This is validated externally when the key is available
    }

    private fun validateCompatibility(
        manifest: KnowledgePackManifest,
        currentAppVersion: String,
        currentSchemaVersion: String,
        errors: MutableList<PackValidationError>,
    ) {
        // App version compatibility
        if (manifest.minimumAppVersion != null && currentAppVersion.isNotBlank()) {
            if (compareVersions(currentAppVersion, manifest.minimumAppVersion) < 0) {
                errors.add(PackValidationError(PackValidationErrorType.APP_INCOMPATIBLE, "App version $currentAppVersion < required ${manifest.minimumAppVersion}"))
            }
        }

        // Schema version compatibility
        if (manifest.minimumSchemaVersion != null && currentSchemaVersion.isNotBlank()) {
            if (compareVersions(currentSchemaVersion, manifest.minimumSchemaVersion) < 0) {
                errors.add(PackValidationError(PackValidationErrorType.SCHEMA_INCOMPATIBLE, "Schema version $currentSchemaVersion < required ${manifest.minimumSchemaVersion}"))
            }
        }
    }

    /**
     * Simple version comparison (major.minor.patch).
     * Returns negative if v1 < v2, 0 if equal, positive if v1 > v2.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
