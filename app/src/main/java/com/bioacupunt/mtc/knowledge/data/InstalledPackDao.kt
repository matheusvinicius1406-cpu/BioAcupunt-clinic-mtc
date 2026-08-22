package com.bioacupunt.mtc.knowledge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for installed knowledge packs.
 *
 * All queries filter by tenantId for multi-tenant isolation.
 * Soft delete pattern: deleted = false for active records.
 */
@Dao
interface InstalledPackDao {

    @Query("SELECT * FROM installed_packs WHERE tenantId = :tenantId AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getAll(tenantId: Long): List<InstalledPackEntity>

    @Query("SELECT * FROM installed_packs WHERE tenantId = :tenantId AND packId = :packId AND deleted = 0 ORDER BY version DESC")
    suspend fun getByPackId(tenantId: Long, packId: String): List<InstalledPackEntity>

    @Query("SELECT * FROM installed_packs WHERE tenantId = :tenantId AND packId = :packId AND status = 'ACTIVE' AND deleted = 0 LIMIT 1")
    suspend fun getActive(tenantId: Long, packId: String): InstalledPackEntity?

    @Query("SELECT * FROM installed_packs WHERE tenantId = :tenantId AND status = 'ACTIVE' AND deleted = 0")
    suspend fun getAllActive(tenantId: Long): List<InstalledPackEntity>

    @Query("SELECT * FROM installed_packs WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): InstalledPackEntity?

    @Query("SELECT * FROM installed_packs WHERE tenantId = :tenantId AND packId = :packId AND version = :version AND deleted = 0 LIMIT 1")
    suspend fun getByPackIdAndVersion(tenantId: Long, packId: String, version: String): InstalledPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InstalledPackEntity): Long

    @Update
    suspend fun update(entity: InstalledPackEntity)

    @Query("UPDATE installed_packs SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: String)

    @Query("UPDATE installed_packs SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: String)

    @Query("SELECT COUNT(*) FROM installed_packs WHERE tenantId = :tenantId AND packId = :packId AND deleted = 0")
    suspend fun countByPackId(tenantId: Long, packId: String): Int
}
