package com.bioacupunt.clinic.data.local

import com.bioacupunt.clinic.domain.model.TongueBodyColor
import com.bioacupunt.clinic.domain.model.TongueCoating
import com.bioacupunt.clinic.domain.model.TongueMoisture
import com.bioacupunt.clinic.domain.model.TongueObservation
import com.bioacupunt.clinic.domain.model.TongueObservationSource
import com.bioacupunt.clinic.domain.model.TongueObservationStatus
import com.bioacupunt.clinic.domain.model.TongueShape
import com.bioacupunt.clinic.domain.repository.TongueObservationRepository
import java.time.Instant

class RoomTongueObservationRepository(
    private val dao: TongueObservationDao,
    private val tenantId: Long,
) : TongueObservationRepository {

    override suspend fun save(observation: TongueObservation): Result<TongueObservation> = runCatching {
        val now = Instant.now().toString()
        val entity = observation.toEntity(tenantId).let {
            if (it.id == 0L) it.copy(createdAt = now, updatedAt = now)
            else it.copy(updatedAt = now)
        }
        val id = dao.insert(entity)
        entity.copy(id = id).toDomain()
    }

    override suspend fun getById(id: Long): Result<TongueObservation?> = runCatching {
        dao.getById(id, tenantId)?.toDomain()
    }

    override suspend fun getByPatient(patientId: Long): Result<List<TongueObservation>> = runCatching {
        dao.getByPatient(tenantId, patientId).map { it.toDomain() }
    }

    override suspend fun getByEncounter(encounterId: Long): Result<List<TongueObservation>> = runCatching {
        dao.getByEncounter(tenantId, encounterId).map { it.toDomain() }
    }

    override suspend fun getLatestConfirmed(patientId: Long): Result<TongueObservation?> = runCatching {
        dao.getLatestConfirmed(tenantId, patientId)?.toDomain()
    }

    override suspend fun updateStatus(id: Long, status: String): Result<Unit> = runCatching {
        dao.updateStatus(id, tenantId, status, Instant.now().toString())
    }

    override suspend fun markReviewed(id: Long, reviewedBy: String): Result<Unit> = runCatching {
        val now = Instant.now().toString()
        dao.markReviewed(id, tenantId, reviewedBy, now, now)
    }

    override suspend fun confirm(id: Long, confirmedBy: String): Result<Unit> = runCatching {
        val now = Instant.now().toString()
        dao.confirm(id, tenantId, confirmedBy, now, now)
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        val now = Instant.now().toString()
        dao.softDelete(id, tenantId, now, now)
    }

    override suspend fun countByPatient(patientId: Long): Result<Int> = runCatching {
        dao.countByPatient(tenantId, patientId)
    }

    // --- Mappers ---

    private fun TongueObservation.toEntity(tid: Long) = TongueObservationEntity(
        id = id,
        tenantId = tid,
        patientId = patientId,
        encounterId = encounterId ?: 0L,
        mediaId = mediaId ?: 0L,
        observationId = observationId ?: 0L,
        bodyColor = bodyColor?.name ?: "",
        bodyColorNotes = bodyColorNotes,
        shape = shape?.name ?: "",
        shapeNotes = shapeNotes,
        coating = coating?.name ?: "",
        coatingNotes = coatingNotes,
        moisture = moisture?.name ?: "",
        moistureNotes = moistureNotes,
        cracks = cracks,
        marks = marks,
        movement = movement,
        specialFindings = specialFindings,
        regionTip = regionTip,
        regionCenter = regionCenter,
        regionRoot = regionRoot,
        regionLeft = regionLeft,
        regionRight = regionRight,
        status = status.name,
        source = source.name,
        visionModelName = visionModelName ?: "",
        visionModelVersion = visionModelVersion ?: "",
        visionConfidence = visionConfidence ?: 0.0,
        preprocessingVersion = preprocessingVersion ?: "",
        reviewedBy = reviewedBy ?: "",
        reviewedAt = reviewedAt ?: "",
        confirmedBy = confirmedBy ?: "",
        confirmedAt = confirmedAt ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = if (deletedAt != null) 1L else 0L,
    )

    private fun TongueObservationEntity.toDomain() = TongueObservation(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        encounterId = encounterId.takeIf { it != 0L },
        mediaId = mediaId.takeIf { it != 0L },
        observationId = observationId.takeIf { it != 0L },
        bodyColor = runCatching { TongueBodyColor.valueOf(bodyColor) }.getOrNull(),
        bodyColorNotes = bodyColorNotes,
        shape = runCatching { TongueShape.valueOf(shape) }.getOrNull(),
        shapeNotes = shapeNotes,
        coating = runCatching { TongueCoating.valueOf(coating) }.getOrNull(),
        coatingNotes = coatingNotes,
        moisture = runCatching { TongueMoisture.valueOf(moisture) }.getOrNull(),
        moistureNotes = moistureNotes,
        cracks = cracks,
        marks = marks,
        movement = movement,
        specialFindings = specialFindings,
        regionTip = regionTip,
        regionCenter = regionCenter,
        regionRoot = regionRoot,
        regionLeft = regionLeft,
        regionRight = regionRight,
        status = runCatching { TongueObservationStatus.valueOf(status) }.getOrDefault(TongueObservationStatus.DRAFT),
        source = runCatching { TongueObservationSource.valueOf(source) }.getOrDefault(TongueObservationSource.MANUAL),
        visionModelName = visionModelName.takeIf { it.isNotEmpty() },
        visionModelVersion = visionModelVersion.takeIf { it.isNotEmpty() },
        visionConfidence = visionConfidence.takeIf { it > 0.0 },
        preprocessingVersion = preprocessingVersion.takeIf { it.isNotEmpty() },
        reviewedBy = reviewedBy.takeIf { it.isNotEmpty() },
        reviewedAt = reviewedAt.takeIf { it.isNotEmpty() },
        confirmedBy = confirmedBy.takeIf { it.isNotEmpty() },
        confirmedAt = confirmedAt.takeIf { it.isNotEmpty() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = if (deleted != 0L) updatedAt else null,
    )
}
