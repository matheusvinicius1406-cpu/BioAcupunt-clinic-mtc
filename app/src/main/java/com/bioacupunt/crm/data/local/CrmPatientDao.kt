package com.bioacupunt.crm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.crm.domain.model.PatientStage
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmPatientDao {

    @Query("SELECT * FROM crm_patients WHERE tenantId = :tenantId AND deleted = 0 ORDER BY updatedAt DESC")
    fun getAll(tenantId: Long): Flow<List<CrmPatientEntity>>

    @Query("SELECT * FROM crm_patients WHERE id = :id AND tenantId = :tenantId LIMIT 1")
    suspend fun getById(id: Long, tenantId: Long): CrmPatientEntity?

    @Query(
        "SELECT * FROM crm_patients WHERE tenantId = :tenantId AND deleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY updatedAt DESC"
    )
    fun search(tenantId: Long, query: String): Flow<List<CrmPatientEntity>>

    @Query("SELECT * FROM crm_patients WHERE tenantId = :tenantId AND deleted = 0 AND stage = :stage ORDER BY updatedAt DESC")
    fun getByStage(tenantId: Long, stage: String): Flow<List<CrmPatientEntity>>

    @Query("SELECT COUNT(*) FROM crm_patients WHERE tenantId = :tenantId AND deleted = 0")
    suspend fun count(tenantId: Long): Int

    @Query("SELECT COUNT(*) FROM crm_patients WHERE tenantId = :tenantId AND deleted = 0 AND stage = :stage")
    suspend fun countByStage(tenantId: Long, stage: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: CrmPatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(entities: List<CrmPatientEntity>)

    @Query("UPDATE crm_patients SET pendingSync = 1, lastModified = :ts WHERE id = :id AND tenantId = :tenantId")
    suspend fun markPending(id: Long, ts: String, tenantId: Long)

    @Query("UPDATE crm_patients SET deleted = 1, pendingSync = 1, updatedAt = :ts, lastModified = :ts WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, ts: String)

    @Query("SELECT * FROM crm_patients WHERE tenantId = :tenantId AND pendingSync = 1 AND deleted = 0 AND lastModified >= :since ORDER BY lastModified ASC")
    suspend fun getChangedSince(tenantId: Long, since: String): List<CrmPatientEntity>

    @Query("SELECT COUNT(*) FROM crm_patients WHERE tenantId = :tenantId AND pendingSync = 1 AND deleted = 0")
    suspend fun pendingCount(tenantId: Long): Int

    // ── Sync ─────────────────────────────────────────────────────────────
    // Everything below is used only by SyncEngine.

    /** Every locally-changed row awaiting upload, oldest change first. */
    @Query("SELECT * FROM crm_patients WHERE pendingSync = 1 ORDER BY lastModified ASC")
    suspend fun getPendingSync(): List<CrmPatientEntity>

    @Query("SELECT * FROM crm_patients WHERE clientId = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): CrmPatientEntity?

    @Query("SELECT * FROM crm_patients WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): CrmPatientEntity?

    /**
     * Records that the server accepted this row at revision [rev].
     *
     * Clears pendingSync in the *same statement* that stores the revision.
     * Splitting them would leave a window where a crash marks the row synced
     * without recording what it synced to — and the next push would then send a
     * stale base revision and be refused as a conflict the doctor never caused.
     */
    @Query("UPDATE crm_patients SET pendingSync = 0, serverId = :serverId, baseRev = :rev WHERE clientId = :clientId")
    suspend fun markSynced(clientId: String, serverId: Long?, rev: Long)

    /**
     * Adopts the server's revision while keeping the local field values, so a
     * conflict resolved in favour of this device can be pushed again without
     * being refused as stale. pendingSync stays 1 on purpose: the row still has
     * to go up.
     */
    @Query("UPDATE crm_patients SET serverId = :serverId, baseRev = :rev, pendingSync = 1 WHERE clientId = :clientId")
    suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long)
}
