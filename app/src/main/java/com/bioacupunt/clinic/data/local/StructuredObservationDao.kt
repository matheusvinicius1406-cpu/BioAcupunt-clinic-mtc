package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface StructuredObservationDao {
    @Query("SELECT * FROM structured_observations WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): StructuredObservationEntity?

    @Query("SELECT * FROM structured_observations WHERE encounterId = :encounterId AND deleted = 0 ORDER BY createdAt ASC")
    suspend fun getByEncounterId(encounterId: Long): List<StructuredObservationEntity>

    @Query("SELECT * FROM structured_observations WHERE patientId = :patientId AND deleted = 0 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByPatientId(patientId: Long, limit: Int = 50): List<StructuredObservationEntity>

    @Query("SELECT * FROM structured_observations WHERE patientId = :patientId AND type = :type AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getByPatientIdAndType(patientId: Long, type: String): List<StructuredObservationEntity>

    @Query("SELECT * FROM structured_observations WHERE patientId = :patientId AND status = :status AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getByPatientIdAndStatus(patientId: Long, status: String): List<StructuredObservationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StructuredObservationEntity): Long

    @Update
    suspend fun update(entity: StructuredObservationEntity)

    @Query("UPDATE structured_observations SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: String)
}
