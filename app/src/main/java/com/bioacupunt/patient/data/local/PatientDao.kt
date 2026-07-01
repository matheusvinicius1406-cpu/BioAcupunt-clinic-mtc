package com.bioacupunt.patient.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(patient: PatientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(patients: List<PatientEntity>)

    @Query("UPDATE patients SET pendingSync = 1 WHERE id = :id")
    suspend fun markPendingSync(id: Long)

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun count(): Int
}
