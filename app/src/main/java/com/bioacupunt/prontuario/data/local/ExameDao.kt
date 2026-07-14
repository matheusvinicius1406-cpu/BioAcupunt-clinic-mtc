package com.bioacupunt.prontuario.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExameDao {
    @Query("SELECT * FROM vital_signs WHERE patientId = :pid ORDER BY recordedAt DESC, id DESC")
    fun observeVitals(pid: Long): Flow<List<VitalSignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVital(entity: VitalSignEntity): Long

    @Query("DELETE FROM vital_signs WHERE id = :id")
    suspend fun deleteVital(id: Long)

    @Query("SELECT * FROM lab_exams WHERE patientId = :pid ORDER BY date DESC, id DESC")
    fun observeExams(pid: Long): Flow<List<LabExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExam(entity: LabExamEntity): Long

    @Query("DELETE FROM lab_exams WHERE id = :id")
    suspend fun deleteExam(id: Long)

    @Query("SELECT * FROM medications WHERE patientId = :pid ORDER BY active DESC, id DESC")
    fun observeMedications(pid: Long): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMedication(entity: MedicationEntity): Long

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Long)

    @Query("SELECT * FROM allergies WHERE patientId = :pid ORDER BY id DESC")
    fun observeAllergies(pid: Long): Flow<List<AllergyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllergy(entity: AllergyEntity): Long

    @Query("DELETE FROM allergies WHERE id = :id")
    suspend fun deleteAllergy(id: Long)
}
