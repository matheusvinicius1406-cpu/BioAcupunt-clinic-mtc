package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.QuestionnaireResponse

interface QuestionnaireResponseRepository {
    suspend fun getById(id: Long): QuestionnaireResponse?
    suspend fun getByPatientId(patientId: Long): List<QuestionnaireResponse>
    suspend fun getByQuestionnaireAndPatient(questionnaireId: String, patientId: Long): List<QuestionnaireResponse>
    suspend fun getByEncounterId(encounterId: Long): List<QuestionnaireResponse>
    suspend fun create(response: QuestionnaireResponse): Long
    suspend fun update(response: QuestionnaireResponse)
}
