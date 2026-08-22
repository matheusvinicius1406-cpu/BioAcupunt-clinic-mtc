package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.mtc.knowledge.domain.InstalledPack
import com.bioacupunt.mtc.knowledge.domain.PackStatus
import com.bioacupunt.mtc.knowledge.repository.InstalledPackRepository

/**
 * Room implementation of [InstalledPackRepository].
 *
 * Maps between domain [InstalledPack] and Room [InstalledPackEntity].
 * All operations are wrapped in runCatching to prevent exception leakage.
 */
class RoomInstalledPackRepository(
    private val dao: InstalledPackDao,
) : InstalledPackRepository {

    override suspend fun getAll(tenantId: Long): Result<List<InstalledPack>> = runCatching {
        dao.getAll(tenantId).map { it.toDomain() }
    }

    override suspend fun getByPackId(tenantId: Long, packId: String): Result<List<InstalledPack>> = runCatching {
        dao.getByPackId(tenantId, packId).map { it.toDomain() }
    }

    override suspend fun getActive(tenantId: Long, packId: String): Result<InstalledPack?> = runCatching {
        dao.getActive(tenantId, packId)?.toDomain()
    }

    override suspend fun getAllActive(tenantId: Long): Result<List<InstalledPack>> = runCatching {
        dao.getAllActive(tenantId).map { it.toDomain() }
    }

    override suspend fun getById(id: Long): Result<InstalledPack?> = runCatching {
        dao.getById(id)?.toDomain()
    }

    override suspend fun getByPackIdAndVersion(tenantId: Long, packId: String, version: String): Result<InstalledPack?> = runCatching {
        dao.getByPackIdAndVersion(tenantId, packId, version)?.toDomain()
    }

    override suspend fun insert(pack: InstalledPack): Result<Long> = runCatching {
        dao.insert(pack.toEntity())
    }

    override suspend fun update(pack: InstalledPack): Result<Unit> = runCatching {
        dao.update(pack.toEntity())
    }

    override suspend fun updateStatus(id: Long, status: PackStatus): Result<Unit> = runCatching {
        dao.updateStatus(id, status.name, java.time.Instant.now().toString())
    }

    override suspend fun softDelete(id: Long): Result<Unit> = runCatching {
        dao.softDelete(id, java.time.Instant.now().toString())
    }

    override suspend fun countByPackId(tenantId: Long, packId: String): Result<Int> = runCatching {
        dao.countByPackId(tenantId, packId)
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun InstalledPackEntity.toDomain() = InstalledPack(
        id = id,
        tenantId = tenantId,
        packId = packId,
        version = version,
        status = PackStatus.valueOf(status),
        manifestJson = manifestJson,
        checksum = checksum,
        installedAt = installedAt,
        activatedAt = activatedAt,
        deactivatedAt = deactivatedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun InstalledPack.toEntity() = InstalledPackEntity(
        id = id,
        tenantId = tenantId,
        packId = packId,
        version = version,
        status = status.name,
        manifestJson = manifestJson,
        checksum = checksum,
        installedAt = installedAt,
        activatedAt = activatedAt,
        deactivatedAt = deactivatedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
