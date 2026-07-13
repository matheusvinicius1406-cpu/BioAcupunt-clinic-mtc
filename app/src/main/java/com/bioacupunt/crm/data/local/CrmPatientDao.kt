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
}
