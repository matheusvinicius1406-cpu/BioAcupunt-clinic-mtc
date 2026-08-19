package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): EncounterEntity?

    @Query("SELECT * FROM encounters WHERE patientId = :patientId AND deleted = 0 ORDER BY startedAt DESC")
    suspend fun getByPatientId(patientId: Long): List<EncounterEntity>

    @Query("SELECT * FROM encounters WHERE tenantId = :tenantId AND deleted = 0 ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getByTenantId(tenantId: Long, limit: Int = 50): List<EncounterEntity>

    @Query("SELECT * FROM encounters WHERE patientId = :patientId AND status = :status AND deleted = 0")
    suspend fun getByPatientIdAndStatus(patientId: Long, status: String): List<EncounterEntity>

    @Query("SELECT * FROM encounters WHERE tenantId = :tenantId AND deleted = 0 ORDER BY startedAt DESC")
    fun observeByTenantId(tenantId: Long): kotlinx.coroutines.flow.Flow<List<EncounterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EncounterEntity): Long

    @Update
    suspend fun update(entity: EncounterEntity)

    @Query("UPDATE encounters SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: String)

    @Query("SELECT COUNT(*) FROM encounters WHERE patientId = :patientId AND deleted = 0")
    suspend fun countByPatientId(patientId: Long): Int
}
