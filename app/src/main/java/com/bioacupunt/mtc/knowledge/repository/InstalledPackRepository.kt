package com.bioacupunt.mtc.knowledge.repository

import com.bioacupunt.mtc.knowledge.domain.InstalledPack
import com.bioacupunt.mtc.knowledge.domain.PackStatus

/**
 * Repository for installed knowledge packs.
 *
 * Provides CRUD operations for pack installation lifecycle management.
 * All operations are tenant-scoped.
 */
interface InstalledPackRepository {

    /**
     * Get all installed packs for a tenant.
     */
    suspend fun getAll(tenantId: Long): Result<List<InstalledPack>>

    /**
     * Get all versions of a specific pack.
     */
    suspend fun getByPackId(tenantId: Long, packId: String): Result<List<InstalledPack>>

    /**
     * Get the active version of a specific pack.
     */
    suspend fun getActive(tenantId: Long, packId: String): Result<InstalledPack?>

    /**
     * Get all active packs.
     */
    suspend fun getAllActive(tenantId: Long): Result<List<InstalledPack>>

    /**
     * Get a specific installed pack by ID.
     */
    suspend fun getById(id: Long): Result<InstalledPack?>

    /**
     * Get a specific version of a pack.
     */
    suspend fun getByPackIdAndVersion(tenantId: Long, packId: String, version: String): Result<InstalledPack?>

    /**
     * Insert a new installed pack record.
     */
    suspend fun insert(pack: InstalledPack): Result<Long>

    /**
     * Update an installed pack record.
     */
    suspend fun update(pack: InstalledPack): Result<Unit>

    /**
     * Update the status of an installed pack.
     */
    suspend fun updateStatus(id: Long, status: PackStatus): Result<Unit>

    /**
     * Soft-delete an installed pack.
     */
    suspend fun softDelete(id: Long): Result<Unit>

    /**
     * Count installed versions of a pack.
     */
    suspend fun countByPackId(tenantId: Long, packId: String): Result<Int>
}
