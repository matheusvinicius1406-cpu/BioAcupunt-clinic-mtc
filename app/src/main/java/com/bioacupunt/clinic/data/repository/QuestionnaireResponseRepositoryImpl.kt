package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.QuestionnaireResponseDao
import com.bioacupunt.clinic.data.local.QuestionnaireResponseEntity
import com.bioacupunt.clinic.domain.model.QuestionnaireResponse
import com.bioacupunt.clinic.domain.model.ResponseStatus
import com.bioacupunt.clinic.domain.repository.QuestionnaireResponseRepository

class QuestionnaireResponseRepositoryImpl(
    private val dao: QuestionnaireResponseDao,
    private val tenantId: () -> Long,
) : QuestionnaireResponseRepository {

    override suspend fun getById(id: Long): QuestionnaireResponse? =
        dao.getById(id)?.toDomain()

    override suspend fun getByPatientId(patientId: Long): List<QuestionnaireResponse> =
        dao.getByPatientId(patientId).map { it.toDomain() }

    override suspend fun getByQuestionnaireAndPatient(questionnaireId: String, patientId: Long): List<QuestionnaireResponse> =
        dao.getByQuestionnaireAndPatient(questionnaireId, patientId).map { it.toDomain() }

    override suspend fun getByEncounterId(encounterId: Long): List<QuestionnaireResponse> =
        dao.getByEncounterId(encounterId).map { it.toDomain() }

    override suspend fun create(response: QuestionnaireResponse): Long =
        dao.insert(response.toEntity(tenantId()))

    override suspend fun update(response: QuestionnaireResponse) =
        dao.update(response.toEntity(tenantId()))

    private fun QuestionnaireResponseEntity.toDomain() = QuestionnaireResponse(
        id = id,
        tenantId = tenantId,
        questionnaireId = questionnaireId,
        questionnaireVersion = questionnaireVersion,
        patientId = patientId,
        encounterId = encounterId,
        status = ResponseStatus.entries.firstOrNull { it.name == status } ?: ResponseStatus.IN_PROGRESS,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun QuestionnaireResponse.toEntity(tid: Long) = QuestionnaireResponseEntity(
        id = id,
        tenantId = tid,
        questionnaireId = questionnaireId,
        questionnaireVersion = questionnaireVersion,
        patientId = patientId,
        encounterId = encounterId,
        answersJson = "{}",
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
