package com.bioacupunt.clinic.data.local

import com.bioacupunt.clinic.domain.model.PulseInputProvider
import com.bioacupunt.clinic.domain.model.PulseObservation
import com.bioacupunt.clinic.domain.model.PulseObservationStatus
import com.bioacupunt.clinic.domain.repository.PulseObservationRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class RoomPulseObservationRepository(
    private val dao: PulseObservationDao,
    private val tenantId: Long,
) : PulseObservationRepository {

    override suspend fun save(observation: PulseObservation): Result<PulseObservation> = runCatching {
        val now = Instant.now().toString()
        val entity = observation.toEntity(tenantId).let {
            if (it.id == 0L) it.copy(createdAt = now, updatedAt = now)
            else it.copy(updatedAt = now)
        }
        val id = dao.insert(entity)
        entity.copy(id = id).toDomain()
    }

    override suspend fun getById(id: Long): Result<PulseObservation?> = runCatching {
        dao.getById(id, tenantId)?.toDomain()
    }

    override suspend fun getByPatient(patientId: Long): Result<List<PulseObservation>> = runCatching {
        dao.getByPatient(tenantId, patientId).map { it.toDomain() }
    }

    override suspend fun getByEncounter(encounterId: Long): Result<List<PulseObservation>> = runCatching {
        dao.getByEncounter(tenantId, encounterId).map { it.toDomain() }
    }

    override suspend fun getLatestConfirmed(patientId: Long): Result<PulseObservation?> = runCatching {
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

    private fun PulseObservation.toEntity(tid: Long) = PulseObservationEntity(
        id = id,
        tenantId = tid,
        patientId = patientId,
        encounterId = encounterId ?: 0L,
        observationId = observationId ?: 0L,
        depth = depth,
        rate = rate ?: 0,
        strength = strength,
        width = width,
        quality = quality,
        qualityNotes = qualityNotes,
        leftCun = leftCun,
        leftGuan = leftGuan,
        leftChi = leftChi,
        rightCun = rightCun,
        rightGuan = rightGuan,
        rightChi = rightChi,
        featuresJson = featuresToJson(features),
        status = status.name,
        source = source.name,
        reviewedBy = reviewedBy ?: "",
        reviewedAt = reviewedAt ?: "",
        confirmedBy = confirmedBy ?: "",
        confirmedAt = confirmedAt ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = if (deletedAt != null) 1L else 0L,
    )

    private fun PulseObservationEntity.toDomain() = PulseObservation(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        encounterId = encounterId.takeIf { it != 0L },
        observationId = observationId.takeIf { it != 0L },
        depth = depth,
        rate = rate.takeIf { it > 0 },
        strength = strength,
        width = width,
        quality = quality,
        qualityNotes = qualityNotes,
        leftCun = leftCun,
        leftGuan = leftGuan,
        leftChi = leftChi,
        rightCun = rightCun,
        rightGuan = rightGuan,
        rightChi = rightChi,
        features = jsonToFeatures(featuresJson),
        status = runCatching { PulseObservationStatus.valueOf(status) }.getOrDefault(PulseObservationStatus.DRAFT),
        source = runCatching { PulseInputProvider.valueOf(source) }.getOrDefault(PulseInputProvider.MANUAL),
        reviewedBy = reviewedBy.takeIf { it.isNotEmpty() },
        reviewedAt = reviewedAt.takeIf { it.isNotEmpty() },
        confirmedBy = confirmedBy.takeIf { it.isNotEmpty() },
        confirmedAt = confirmedAt.takeIf { it.isNotEmpty() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = if (deleted != 0L) updatedAt else null,
    )

    private fun featuresToJson(features: List<com.bioacupunt.clinic.domain.model.PulseFeature>): String {
        val arr = JSONArray()
        features.forEach { f ->
            arr.put(JSONObject().apply {
                put("name", f.name)
                put("value", f.value)
                put("unit", f.unit)
                put("confidence", f.confidence ?: 0.0)
                put("source", f.source.name)
            })
        }
        return arr.toString()
    }

    private fun jsonToFeatures(json: String): List<com.bioacupunt.clinic.domain.model.PulseFeature> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                com.bioacupunt.clinic.domain.model.PulseFeature(
                    name = obj.getString("name"),
                    value = obj.getString("value"),
                    unit = obj.optString("unit", ""),
                    confidence = obj.optDouble("confidence", 0.0).takeIf { it > 0.0 },
                    source = runCatching {
                        PulseInputProvider.valueOf(obj.getString("source"))
                    }.getOrDefault(PulseInputProvider.MANUAL),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
