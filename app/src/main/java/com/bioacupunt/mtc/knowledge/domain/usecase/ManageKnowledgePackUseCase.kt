package com.bioacupunt.mtc.knowledge.domain.usecase

import com.bioacupunt.mtc.knowledge.domain.InstalledPack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackManifest
import com.bioacupunt.mtc.knowledge.domain.PackAuditEvent
import com.bioacupunt.mtc.knowledge.domain.PackAuditEventType
import com.bioacupunt.mtc.knowledge.domain.PackDiff
import com.bioacupunt.mtc.knowledge.domain.PackDiffItem
import com.bioacupunt.mtc.knowledge.domain.PackChangeType
import com.bioacupunt.mtc.knowledge.domain.PackStatus
import com.bioacupunt.mtc.knowledge.domain.PackValidationResult
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackValidator
import com.bioacupunt.mtc.knowledge.repository.InstalledPackRepository
import java.time.Instant

/**
 * Use case for managing knowledge pack lifecycle:
 * - Validate pack integrity
 * - Install (stage) a pack
 * - Activate a staged pack
 * - Rollback to a previous version
 * - Compare pack versions (diff)
 * - Track provenance
 *
 * The pack lifecycle is:
 * DOWNLOADED → VALIDATING → VALID → STAGED → ACTIVE
 *
 * Rollback:
 * ACTIVE → ROLLBACK → previous ACTIVE
 *
 * All operations are tenant-scoped.
 */
class ManageKnowledgePackUseCase(
    private val installedPackRepository: InstalledPackRepository,
    private val validator: KnowledgePackValidator = KnowledgePackValidator(),
) {

    /**
     * Validate a knowledge pack.
     *
     * @param pack The pack to validate
     * @param tenantId The tenant ID
     * @return Validation result
     */
    suspend fun validate(pack: KnowledgePack, tenantId: Long): Result<PackValidationResult> = runCatching {
        validator.validate(pack)
    }

    /**
     * Install (stage) a knowledge pack.
     *
     * This does NOT activate the pack — it only marks it as STAGED.
     * Activation is a separate step to ensure atomicity.
     *
     * @param pack The pack to install
     * @param tenantId The tenant ID
     * @return The installed pack record
     */
    suspend fun install(pack: KnowledgePack, tenantId: Long): Result<InstalledPack> = runCatching {
        val now = Instant.now().toString()

        // Validate first
        val validation = validator.validate(pack)
        if (!validation.isValid) {
            throw IllegalStateException("Pack validation failed: ${validation.errors.map { it.message }}")
        }

        // Check if this exact version is already installed
        val existing = installedPackRepository.getByPackIdAndVersion(
            tenantId, pack.manifest.packId, pack.manifest.version
        ).getOrNull()
        if (existing != null && existing.status != PackStatus.ROLLBACK) {
            return@runCatching existing
        }

        // Insert new installed pack record
        val installed = InstalledPack(
            tenantId = tenantId,
            packId = pack.manifest.packId,
            version = pack.manifest.version,
            status = PackStatus.STAGED,
            manifestJson = pack.manifest.toString(),
            checksum = pack.manifest.checksum,
            installedAt = now,
            createdAt = now,
            updatedAt = now,
        )

        installedPackRepository.insert(installed).getOrThrow()
        installed.copy(id = installedPackRepository.getByPackIdAndVersion(
            tenantId, pack.manifest.packId, pack.manifest.version
        ).getOrThrow()?.id ?: 0)
    }

    /**
     * Activate a staged pack.
     *
     * This deactivates the current active version and activates the new one.
     * The operation is idempotent — activating an already active pack is a no-op.
     *
     * @param packId The pack ID
     * @param version The version to activate
     * @param tenantId The tenant ID
     * @return The activated pack record
     */
    suspend fun activate(packId: String, version: String, tenantId: Long): Result<InstalledPack> = runCatching {
        val now = Instant.now().toString()

        // Find the pack to activate
        val target = installedPackRepository.getByPackIdAndVersion(tenantId, packId, version)
            .getOrThrow()
            ?: throw IllegalStateException("Pack $packId@$version not found")

        if (target.status == PackStatus.ACTIVE) {
            return@runCatching target
        }

        // Deactivate current active version
        val currentActive = installedPackRepository.getActive(tenantId, packId).getOrNull()
        if (currentActive != null && currentActive.id != target.id) {
            installedPackRepository.updateStatus(currentActive.id, PackStatus.INACTIVE).getOrThrow()
        }

        // Activate new version
        installedPackRepository.updateStatus(target.id, PackStatus.ACTIVE).getOrThrow()

        target.copy(status = PackStatus.ACTIVE, activatedAt = now, updatedAt = now)
    }

    /**
     * Rollback to a previous version.
     *
     * Marks the current active as ROLLBACK and activates the target version.
     *
     * @param packId The pack ID
     * @param targetVersion The version to rollback to
     * @param tenantId The tenant ID
     * @return The rolled-back pack record
     */
    suspend fun rollback(packId: String, targetVersion: String, tenantId: Long): Result<InstalledPack> = runCatching {
        val now = Instant.now().toString()

        // Find current active
        val currentActive = installedPackRepository.getActive(tenantId, packId).getOrThrow()
            ?: throw IllegalStateException("No active pack found for $packId")

        // Find target version
        val target = installedPackRepository.getByPackIdAndVersion(tenantId, packId, targetVersion)
            .getOrThrow()
            ?: throw IllegalStateException("Target version $targetVersion not found for $packId")

        // Mark current as ROLLBACK
        installedPackRepository.updateStatus(currentActive.id, PackStatus.ROLLBACK).getOrThrow()

        // Activate target
        installedPackRepository.updateStatus(target.id, PackStatus.ACTIVE).getOrThrow()

        target.copy(status = PackStatus.ACTIVE, activatedAt = now, updatedAt = now)
    }

    /**
     * Compare two versions of a pack.
     *
     * @param oldPack The old pack
     * @param newPack The new pack
     * @return Diff between the two versions
     */
    fun diff(oldPack: KnowledgePack, newPack: KnowledgePack): PackDiff {
        val oldEntityIds = oldPack.entities.map { it.id }.toSet()
        val newEntityIds = newPack.entities.map { it.id }.toSet()

        val added = newPack.entities.filter { it.id !in oldEntityIds }.map {
            PackDiffItem(it.id, it.type, PackChangeType.ADDED, null, it.version.version)
        }

        val removed = oldPack.entities.filter { it.id !in newEntityIds }.map {
            PackDiffItem(it.id, it.type, PackChangeType.REMOVED, it.version.version, null)
        }

        val changed = newPack.entities.filter { it.id in oldEntityIds }.mapNotNull { newEntity ->
            val oldEntity = oldPack.entities.firstOrNull { it.id == newEntity.id }
            if (oldEntity != null && oldEntity.content != newEntity.content) {
                PackDiffItem(newEntity.id, newEntity.type, PackChangeType.CHANGED, oldEntity.version.version, newEntity.version.version)
            } else null
        }

        return PackDiff(
            oldVersion = oldPack.manifest.version,
            newVersion = newPack.manifest.version,
            added = added,
            removed = removed,
            changed = changed,
        )
    }

    /**
     * Get the active pack for a given pack ID.
     */
    suspend fun getActive(packId: String, tenantId: Long): Result<InstalledPack?> = runCatching {
        installedPackRepository.getActive(tenantId, packId).getOrNull()
    }

    /**
     * Get all active packs.
     */
    suspend fun getAllActive(tenantId: Long): Result<List<InstalledPack>> = runCatching {
        installedPackRepository.getAllActive(tenantId).getOrThrow()
    }

    /**
     * Get all versions of a pack.
     */
    suspend fun getAllVersions(packId: String, tenantId: Long): Result<List<InstalledPack>> = runCatching {
        installedPackRepository.getByPackId(tenantId, packId).getOrThrow()
    }
}
