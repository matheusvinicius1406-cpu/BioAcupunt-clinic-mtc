package com.bioacupunt.crm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.crm.domain.model.PatientStage
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmPatientDao {

    @Query("SELECT * FROM crm_patients WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<CrmPatientEntity>>

    @Query("SELECT * FROM crm_patients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CrmPatientEntity?

    @Query(
        "SELECT * FROM crm_patients WHERE deleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY updatedAt DESC"
    )
    fun search(query: String): Flow<List<CrmPatientEntity>>

    @Query("SELECT * FROM crm_patients WHERE deleted = 0 AND stage = :stage ORDER BY updatedAt DESC")
    fun getByStage(stage: String): Flow<List<CrmPatientEntity>>

    @Query("SELECT COUNT(*) FROM crm_patients WHERE deleted = 0")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM crm_patients WHERE deleted = 0 AND stage = :stage")
    suspend fun countByStage(stage: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: CrmPatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(entities: List<CrmPatientEntity>)

    @Query("UPDATE crm_patients SET pendingSync = 1, lastModified = :ts WHERE id = :id")
    suspend fun markPending(id: Long, ts: String)

    @Query("SELECT * FROM crm_patients WHERE pendingSync = 1 AND deleted = 0 AND lastModified >= :since ORDER BY lastModified ASC")
    suspend fun getChangedSince(since: String): List<CrmPatientEntity>

    @Query("SELECT COUNT(*) FROM crm_patients WHERE pendingSync = 1 AND deleted = 0")
    suspend fun pendingCount(): Int
}
