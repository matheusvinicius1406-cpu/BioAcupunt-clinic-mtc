package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.ClinicalNoteDao
import com.bioacupunt.clinic.data.local.ClinicalNoteEntity
import com.bioacupunt.clinic.domain.model.ClinicalNote
import com.bioacupunt.clinic.domain.model.NoteFormat
import com.bioacupunt.clinic.domain.model.NoteStatus
import com.bioacupunt.clinic.domain.repository.ClinicalNoteRepository

class ClinicalNoteRepositoryImpl(
    private val dao: ClinicalNoteDao,
    private val tenantId: () -> Long,
) : ClinicalNoteRepository {

    override suspend fun getById(id: Long): ClinicalNote? =
        dao.getById(id)?.toDomain()

    override suspend fun getByEncounterId(encounterId: Long): ClinicalNote? =
        dao.getByEncounterId(encounterId)?.toDomain()

    override suspend fun getByPatientId(patientId: Long): List<ClinicalNote> =
        dao.getByPatientId(patientId).map { it.toDomain() }

    override suspend fun create(note: ClinicalNote): Long =
        dao.insert(note.toEntity(tenantId()))

    override suspend fun update(note: ClinicalNote) =
        dao.update(note.toEntity(tenantId()))

    override suspend fun finalizeNote(id: Long, finalizedBy: String) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(
                status = NoteStatus.FINAL.name,
                finalizedBy = finalizedBy,
                finalizedAt = now,
                updatedAt = now,
            ))
        }
    }

    override suspend fun delete(id: Long) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(deleted = true, updatedAt = now))
        }
    }

    private fun ClinicalNoteEntity.toDomain() = ClinicalNote(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        format = NoteFormat.entries.firstOrNull { it.name == format } ?: NoteFormat.SOAP,
        status = NoteStatus.entries.firstOrNull { it.name == status } ?: NoteStatus.DRAFT,
        subjective = subjective,
        objective = objective,
        assessment = assessment,
        plan = plan,
        mtcAssessmentSummary = mtcAssessmentSummary,
        references = emptyList(),
        createdBy = createdBy,
        finalizedBy = finalizedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        finalizedAt = finalizedAt,
    )

    private fun ClinicalNote.toEntity(tid: Long) = ClinicalNoteEntity(
        id = id,
        tenantId = tid,
        encounterId = encounterId,
        patientId = patientId,
        format = format.name,
        subjective = subjective,
        objective = objective,
        assessment = assessment,
        plan = plan,
        mtcAssessmentSummary = mtcAssessmentSummary,
        referencesJson = "[]",
        status = status.name,
        createdBy = createdBy,
        finalizedBy = finalizedBy,
        finalizedAt = finalizedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = false,
    )
}
