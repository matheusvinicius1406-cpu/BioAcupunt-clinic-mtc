package com.bioacupunt.clinic.data.local

import com.bioacupunt.clinic.domain.model.ClinicalMedia
import com.bioacupunt.clinic.domain.model.ClinicalMediaSource
import com.bioacupunt.clinic.domain.model.ClinicalMediaStatus
import com.bioacupunt.clinic.domain.model.ClinicalMediaType
import com.bioacupunt.clinic.repository.ClinicalMediaRepository
import java.time.Instant

class RoomClinicalMediaRepository(
    private val dao: ClinicalMediaDao,
    private val tenantId: Long,
) : ClinicalMediaRepository {

    override suspend fun save(media: ClinicalMedia): Result<ClinicalMedia> = runCatching {
        val now = Instant.now().toString()
        val entity = media.toEntity(tenantId).let {
            if (it.id == 0L) it.copy(createdAt = now, updatedAt = now)
            else it.copy(updatedAt = now)
        }
        val id = dao.insert(entity)
        entity.copy(id = id).toDomain()
    }

    override suspend fun getById(id: Long): Result<ClinicalMedia?> = runCatching {
        dao.getById(id, tenantId)?.toDomain()
    }

    override suspend fun getByPatient(patientId: Long): Result<List<ClinicalMedia>> = runCatching {
        dao.getByPatient(tenantId, patientId).map { it.toDomain() }
    }

    override suspend fun getByEncounter(encounterId: Long): Result<List<ClinicalMedia>> = runCatching {
        dao.getByEncounter(tenantId, encounterId).map { it.toDomain() }
    }

    override suspend fun getByPatientAndCategory(
        patientId: Long,
        category: String
    ): Result<List<ClinicalMedia>> = runCatching {
        dao.getByPatientAndCategory(tenantId, patientId, category).map { it.toDomain() }
    }

    override suspend fun getByStatus(status: ClinicalMediaStatus): Result<List<ClinicalMedia>> = runCatching {
        dao.getByStatus(tenantId, status.name).map { it.toDomain() }
    }

    override suspend fun updateStatus(id: Long, status: ClinicalMediaStatus): Result<Unit> = runCatching {
        dao.updateStatus(id, tenantId, status.name, Instant.now().toString())
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        val now = Instant.now().toString()
        dao.softDelete(id, tenantId, now, now)
    }

    override suspend fun countByPatient(patientId: Long): Result<Int> = runCatching {
        dao.countByPatient(tenantId, patientId)
    }

    // --- Mappers ---

    private fun ClinicalMedia.toEntity(tid: Long) = ClinicalMediaEntity(
        id = id,
        tenantId = tid,
        patientId = patientId,
        encounterId = encounterId ?: 0L,
        type = type.name,
        uri = uri,
        mimeType = mimeType,
        originalName = originalName ?: "",
        sizeBytes = sizeBytes,
        hash = hash,
        source = source.name,
        status = status.name,
        category = category,
        description = description,
        processingVersion = processingVersion ?: "",
        capturedAt = capturedAt ?: "",
        capturedBy = capturedBy ?: "",
        deviceInfo = deviceInfo ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = if (deletedAt != null) 1L else 0L,
    )

    private fun ClinicalMediaEntity.toDomain() = ClinicalMedia(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        encounterId = encounterId.takeIf { it != 0L },
        type = try { ClinicalMediaType.valueOf(type) } catch (_: Exception) { ClinicalMediaType.IMAGE },
        uri = uri,
        mimeType = mimeType,
        originalName = originalName.takeIf { it.isNotEmpty() },
        sizeBytes = sizeBytes,
        hash = hash,
        source = try { ClinicalMediaSource.valueOf(source) } catch (_: Exception) { ClinicalMediaSource.CAMERA },
        status = try { ClinicalMediaStatus.valueOf(status) } catch (_: Exception) { ClinicalMediaStatus.CAPTURED },
        category = category,
        description = description,
        processingVersion = processingVersion.takeIf { it.isNotEmpty() },
        capturedAt = capturedAt.takeIf { it.isNotEmpty() },
        capturedBy = capturedBy.takeIf { it.isNotEmpty() },
        deviceInfo = deviceInfo.takeIf { it.isNotEmpty() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = if (deleted != 0L) updatedAt else null,
    )
}
