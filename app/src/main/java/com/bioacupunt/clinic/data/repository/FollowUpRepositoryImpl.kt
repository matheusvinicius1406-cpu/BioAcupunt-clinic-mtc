package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.FollowUpDao
import com.bioacupunt.clinic.data.local.FollowUpEntity
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.FollowUpStatus
import com.bioacupunt.clinic.domain.repository.FollowUpRepository

class FollowUpRepositoryImpl(
    private val dao: FollowUpDao,
    private val tenantId: () -> Long,
) : FollowUpRepository {

    override suspend fun getById(id: Long): FollowUp? =
        dao.getById(id)?.toDomain()

    override suspend fun getByPatientId(patientId: Long): List<FollowUp> =
        dao.getByPatientId(patientId).map { it.toDomain() }

    override suspend fun getByPatientIdAndStatus(patientId: Long, status: FollowUpStatus): List<FollowUp> =
        dao.getByPatientIdAndStatus(patientId, status.name).map { it.toDomain() }

    override suspend fun getUpcoming(): List<FollowUp> =
        dao.getUpcoming(tenantId()).map { it.toDomain() }

    override suspend fun create(followUp: FollowUp): Long =
        dao.insert(followUp.toEntity(tenantId()))

    override suspend fun update(followUp: FollowUp) =
        dao.update(followUp.toEntity(tenantId()))

    override suspend fun complete(id: Long, actualFindings: String) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(
                status = FollowUpStatus.COMPLETED.name,
                actualFindings = actualFindings,
                completedAt = now,
                updatedAt = now,
            ))
        }
    }

    override suspend fun delete(id: Long) {
        val now = java.time.Instant.now().toString()
        dao.softDelete(id, now)
    }

    private fun FollowUpEntity.toDomain() = FollowUp(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        encounterId = encounterId,
        scheduledAt = scheduledAt,
        reason = reason,
        expectedFindings = expectedFindings,
        actualFindings = actualFindings,
        status = FollowUpStatus.entries.firstOrNull { it.name == status } ?: FollowUpStatus.SCHEDULED,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun FollowUp.toEntity(tid: Long) = FollowUpEntity(
        id = id,
        tenantId = tid,
        patientId = patientId,
        encounterId = encounterId,
        scheduledAt = scheduledAt,
        reason = reason,
        expectedFindings = expectedFindings,
        actualFindings = actualFindings,
        status = status.name,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
