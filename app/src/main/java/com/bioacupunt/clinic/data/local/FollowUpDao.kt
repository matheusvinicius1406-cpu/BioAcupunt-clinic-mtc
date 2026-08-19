package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): FollowUpEntity?

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId AND deleted = 0 ORDER BY scheduledAt DESC")
    suspend fun getByPatientId(patientId: Long): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId AND status = :status AND deleted = 0 ORDER BY scheduledAt DESC")
    suspend fun getByPatientIdAndStatus(patientId: Long, status: String): List<FollowUpEntity>

    @Query("SELECT * FROM follow_ups WHERE tenantId = :tenantId AND status = 'SCHEDULED' AND deleted = 0 ORDER BY scheduledAt ASC")
    suspend fun getUpcoming(tenantId: Long): List<FollowUpEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FollowUpEntity): Long

    @Update
    suspend fun update(entity: FollowUpEntity)

    @Query("UPDATE follow_ups SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: String)
}
