package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TreatmentPlanDao {
    @Query("SELECT * FROM treatment_plans WHERE id = :id AND deleted = 0")
    suspend fun getById(id: Long): TreatmentPlanEntity?

    @Query("SELECT * FROM treatment_plans WHERE encounterId = :encounterId AND deleted = 0")
    suspend fun getByEncounterId(encounterId: Long): TreatmentPlanEntity?

    @Query("SELECT * FROM treatment_plans WHERE patientId = :patientId AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getByPatientId(patientId: Long): List<TreatmentPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TreatmentPlanEntity): Long

    @Update
    suspend fun update(entity: TreatmentPlanEntity)

    @Query("UPDATE treatment_plans SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: String)
}
