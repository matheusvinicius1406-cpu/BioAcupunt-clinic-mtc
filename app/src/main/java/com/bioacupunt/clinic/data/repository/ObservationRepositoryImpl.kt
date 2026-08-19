package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.StructuredObservationDao
import com.bioacupunt.clinic.data.local.StructuredObservationEntity
import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.repository.ObservationRepository

class ObservationRepositoryImpl(
    private val dao: StructuredObservationDao,
    private val tenantId: () -> Long,
) : ObservationRepository {

    override suspend fun getById(id: Long): StructuredObservation? =
        dao.getById(id)?.toDomain()

    override suspend fun getByEncounterId(encounterId: Long): List<StructuredObservation> =
        dao.getByEncounterId(encounterId).map { it.toDomain() }

    override suspend fun getByPatientId(patientId: Long, limit: Int): List<StructuredObservation> =
        dao.getByPatientId(patientId, limit).map { it.toDomain() }

    override suspend fun getByPatientIdAndType(patientId: Long, type: ObservationType): List<StructuredObservation> =
        dao.getByPatientIdAndType(patientId, type.name).map { it.toDomain() }

    override suspend fun getByPatientIdAndStatus(patientId: Long, status: ObservationStatus): List<StructuredObservation> =
        dao.getByPatientIdAndStatus(patientId, status.name).map { it.toDomain() }

    override suspend fun create(observation: StructuredObservation): Long =
        dao.insert(observation.toEntity(tenantId()))

    override suspend fun update(observation: StructuredObservation) =
        dao.update(observation.toEntity(tenantId()))

    override suspend fun confirm(id: Long, confirmedBy: String) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(
                status = ObservationStatus.CONFIRMED.name,
                confirmedBy = confirmedBy,
                confirmedAt = now,
                updatedAt = now,
            ))
        }
    }

    override suspend fun reject(id: Long, reviewedBy: String) {
        val now = java.time.Instant.now().toString()
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(
                status = ObservationStatus.REJECTED.name,
                reviewedBy = reviewedBy,
                reviewedAt = now,
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

    private fun StructuredObservationEntity.toDomain() = StructuredObservation(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        type = ObservationType.entries.firstOrNull { it.name == type } ?: ObservationType.GENERAL,
        content = content,
        status = ObservationStatus.entries.firstOrNull { it.name == status } ?: ObservationStatus.DRAFT,
        source = ObservationSource.entries.firstOrNull { it.name == source } ?: ObservationSource.MANUAL_ENTRY,
        sourceSpan = sourceSpan,
        confidence = confidence,
        reviewedBy = reviewedBy,
        reviewedAt = reviewedAt,
        confirmedBy = confirmedBy,
        confirmedAt = confirmedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun StructuredObservation.toEntity(tid: Long) = StructuredObservationEntity(
        id = id,
        tenantId = tid,
        encounterId = encounterId,
        patientId = patientId,
        type = type.name,
        content = content,
        structuredDataJson = "{}",
        status = status.name,
        source = source.name,
        sourceSpan = sourceSpan,
        confidence = confidence,
        reviewedBy = reviewedBy,
        reviewedAt = reviewedAt,
        confirmedBy = confirmedBy,
        confirmedAt = confirmedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
