package com.bioacupunt.clinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuestionnaireResponseDao {
    @Query("SELECT * FROM questionnaire_responses WHERE id = :id")
    suspend fun getById(id: Long): QuestionnaireResponseEntity?

    @Query("SELECT * FROM questionnaire_responses WHERE patientId = :patientId ORDER BY createdAt DESC")
    suspend fun getByPatientId(patientId: Long): List<QuestionnaireResponseEntity>

    @Query("SELECT * FROM questionnaire_responses WHERE questionnaireId = :questionnaireId AND patientId = :patientId ORDER BY createdAt DESC")
    suspend fun getByQuestionnaireAndPatient(questionnaireId: String, patientId: Long): List<QuestionnaireResponseEntity>

    @Query("SELECT * FROM questionnaire_responses WHERE encounterId = :encounterId")
    suspend fun getByEncounterId(encounterId: Long): List<QuestionnaireResponseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuestionnaireResponseEntity): Long

    @Update
    suspend fun update(entity: QuestionnaireResponseEntity)
}
