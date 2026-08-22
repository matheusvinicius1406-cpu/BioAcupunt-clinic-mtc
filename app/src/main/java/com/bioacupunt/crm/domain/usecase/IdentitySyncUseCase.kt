package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.crm.domain.model.CrmIdentityMap
import java.time.Instant

/**
 * Identity Synchronization Use Case — handles bidirectional sync between
 * BioAcupunt clinical entities and external CRM systems (Twenty).
 *
 * Uses the CrmIdentityMap to maintain stable references.
 * Never uses last-write-wins for clinical data.
 *
 * Phase 7 — CRM Clinical Integration
 */
class IdentitySyncUseCase(
    private val identityMapRepository: IdentityMapRepository,
    private val auditLogger: CrmAuditLogger
) {

    /**
     * Result of a sync operation.
     */
    sealed class SyncResult {
        data class Synced(val identityMap: CrmIdentityMap, val direction: String) : SyncResult()
        data class Conflict(val local: CrmIdentityMap, val remoteId: Long) : SyncResult()
        data class AlreadySynced(val identityMap: CrmIdentityMap) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    enum class ConflictResolution { KEEP_LOCAL, KEEP_REMOTE, MERGE }

    /**
     * Register a new mapping between a BioAcupunt entity and a CRM record.
     */
    suspend fun registerMapping(
        bioacupuntEntityId: Long,
        bioacupuntEntityType: String,
        crmEntityId: Long,
        entityType: String,
        tenantId: Long
    ): Result<CrmIdentityMap> = runCatching {
        // Check for existing mapping
        val existing = identityMapRepository.findByBioacupuntId(bioacupuntEntityId, tenantId)
        if (existing != null) {
            // If same CRM record, return existing
            if (existing.crmEntityId == crmEntityId) {
                return@runCatching existing
            }
            // Different CRM record — potential duplicate, don't overwrite silently
            throw IllegalStateException(
                "Entity $bioacupuntEntityId already mapped to CRM record ${existing.crmEntityId}. " +
                "Cannot remap to $crmEntityId without explicit conflict resolution."
            )
        }

        val now = Instant.now().toString()
        val mapping = CrmIdentityMap(
            tenantId = tenantId,
            entityType = entityType,
            crmEntityId = crmEntityId,
            bioacupuntEntityId = bioacupuntEntityId,
            bioacupuntEntityType = bioacupuntEntityType,
            createdAt = now,
            updatedAt = now,
            lastSyncedAt = now
        )

        identityMapRepository.save(mapping)

        auditLogger.logRecordCreated(
            tenantId = tenantId,
            userId = "system",
            entityType = "IDENTITY_MAP",
            entityId = mapping.id
        )

        mapping
    }

    /**
     * Look up the CRM entity ID for a BioAcupunt entity.
     */
    suspend fun lookupCrmRecord(
        bioacupuntEntityId: Long,
        tenantId: Long
    ): Result<CrmIdentityMap?> = runCatching {
        identityMapRepository.findByBioacupuntId(bioacupuntEntityId, tenantId)
    }

    /**
     * Look up the BioAcupunt entity for a CRM record.
     */
    suspend fun lookupBioacupuntEntity(
        crmEntityId: Long,
        tenantId: Long
    ): Result<CrmIdentityMap?> = runCatching {
        identityMapRepository.findByCrmRecordId(crmEntityId, tenantId)
    }

    /**
     * Get all mappings for a tenant, optionally filtered by entity type.
     */
    suspend fun getAllMappings(
        tenantId: Long,
        entityType: String? = null
    ): Result<List<CrmIdentityMap>> = runCatching {
        identityMapRepository.findAllByTenant(tenantId, entityType)
    }

    /**
     * Mark a mapping as needing sync (update lastSyncedAt to null).
     */
    suspend fun markSyncPending(
        identityMapId: Long
    ): Result<CrmIdentityMap> = runCatching {
        val mapping = identityMapRepository.findById(identityMapId)
            ?: throw IllegalArgumentException("Identity map not found: $identityMapId")

        val updated = mapping.copy(
            lastSyncedAt = null,
            updatedAt = Instant.now().toString()
        )

        identityMapRepository.save(updated)
        updated
    }

    /**
     * Complete a sync — update lastSyncedAt.
     */
    suspend fun markSyncComplete(
        identityMapId: Long
    ): Result<CrmIdentityMap> = runCatching {
        val mapping = identityMapRepository.findById(identityMapId)
            ?: throw IllegalArgumentException("Identity map not found: $identityMapId")

        val updated = mapping.copy(
            lastSyncedAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )

        identityMapRepository.save(updated)
        updated
    }

    /**
     * Check if a BioAcupunt entity already has a CRM mapping.
     */
    suspend fun hasMapping(
        bioacupuntEntityId: Long,
        tenantId: Long
    ): Result<Boolean> = runCatching {
        identityMapRepository.findByBioacupuntId(bioacupuntEntityId, tenantId) != null
    }

    /**
     * Remove a mapping (soft — just delete from identity map).
     */
    suspend fun removeMapping(
        identityMapId: Long,
        tenantId: Long
    ): Result<Boolean> = runCatching {
        val mapping = identityMapRepository.findById(identityMapId) ?: return@runCatching false
        if (mapping.tenantId != tenantId) {
            throw SecurityException("Cannot remove mapping from different tenant")
        }
        identityMapRepository.delete(identityMapId)
        true
    }
}

/**
 * Repository interface for Identity Map operations.
 * Implementations use Room persistence.
 */
interface IdentityMapRepository {
    suspend fun save(mapping: CrmIdentityMap): Long
    suspend fun findById(id: Long): CrmIdentityMap?
    suspend fun findByBioacupuntId(bioacupuntEntityId: Long, tenantId: Long): CrmIdentityMap?
    suspend fun findByCrmRecordId(crmEntityId: Long, tenantId: Long): CrmIdentityMap?
    suspend fun findAllByTenant(tenantId: Long, entityType: String?): List<CrmIdentityMap>
    suspend fun delete(id: Long)
}
