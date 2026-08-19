package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.EncounterDao
import com.bioacupunt.clinic.data.local.EncounterEntity
import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.EncounterStatus
import com.bioacupunt.clinic.domain.repository.EncounterRepository

class EncounterRepositoryImpl(
    private val dao: EncounterDao,
    private val tenantId: () -> Long,
) : EncounterRepository {

    override suspend fun getById(id: Long): Encounter? =
        dao.getById(id)?.toDomain()

    override suspend fun getByPatientId(patientId: Long): List<Encounter> =
        dao.getByPatientId(patientId).map { it.toDomain() }

    override suspend fun getRecent(patientId: Long, limit: Int): List<Encounter> =
        dao.getByPatientId(patientId).take(limit).map { it.toDomain() }

    override suspend fun getActive(patientId: Long): Encounter? =
        dao.getByPatientIdAndStatus(patientId, EncounterStatus.IN_PROGRESS.name)
            .firstOrNull()?.toDomain()

    override suspend fun create(encounter: Encounter): Long =
        dao.insert(encounter.toEntity())

    override suspend fun update(encounter: Encounter) =
        dao.update(encounter.toEntity())

    override suspend fun complete(id: Long) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(status = EncounterStatus.COMPLETED.name, endedAt = now, updatedAt = now))
        }
    }

    override suspend fun cancel(id: Long) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(status = EncounterStatus.CANCELLED.name, updatedAt = now))
        }
    }

    override suspend fun countByPatientId(patientId: Long): Int =
        dao.countByPatientId(patientId)

    private fun EncounterEntity.toDomain() = Encounter(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        status = EncounterStatus.valueOf(status),
        type = com.bioacupunt.clinic.domain.model.EncounterType.entries
            .firstOrNull { it.name == type } ?: com.bioacupunt.clinic.domain.model.EncounterType.CONSULTATION,
        startedAt = startedAt,
        endedAt = endedAt,
        practitionerId = practitionerId,
        reason = reason,
        appointmentId = appointmentId,
        currentAssessmentId = currentAssessmentId,
        currentNoteId = currentNoteId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = if (deleted) updatedAt else null,
    )

    private fun Encounter.toEntity() = EncounterEntity(
        id = id,
        tenantId = tenantId(),
        patientId = patientId,
        status = status.name,
        type = type.name,
        startedAt = startedAt,
        endedAt = endedAt,
        practitionerId = practitionerId,
        reason = reason,
        appointmentId = appointmentId,
        currentAssessmentId = currentAssessmentId,
        currentNoteId = currentNoteId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deletedAt != null,
    )
}
