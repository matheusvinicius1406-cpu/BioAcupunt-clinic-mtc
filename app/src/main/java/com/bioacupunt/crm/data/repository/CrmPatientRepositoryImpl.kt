package com.bioacupunt.crm.data.repository

import com.bioacupunt.cache.AppCacheManager
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.crm.data.local.CrmPatientDao
import com.bioacupunt.crm.data.local.CrmPatientEntity
import com.bioacupunt.crm.data.local.toDomain
import com.bioacupunt.crm.data.local.toEntity
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class CrmPatientRepositoryImpl(
    private val dao: CrmPatientDao,
    private val cache: AppCacheManager,
    private val tenantManager: TenantManager
) : CrmPatientRepository {

    private val cacheKeyAll = "crm:patients:all"
    private val cacheKeyStagePrefix = "crm:patients:stage:"
    private val tenantId: Long get() = tenantManager.requireTenantId()

    override fun observeAll(): Flow<List<CrmPatient>> {
        return dao.getAll(tenantId)
            .map<List<CrmPatientEntity>, List<CrmPatient>> { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeByStage(stage: String): Flow<List<CrmPatient>> {
        return dao.getByStage(tenantId, stage)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun search(query: String): Flow<List<CrmPatient>> {
        return dao.search(tenantId, query)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun getById(id: Long): Result<CrmPatient> {
        return try {
            val entity = dao.getById(id, tenantId)
            if (entity == null) {
                Result.Error(AppError.DatabaseError())
            } else {
                Result.Success(entity.toDomain())
            }
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun save(entity: CrmPatient): Result<CrmPatient> {
        return try {
            val owned = entity.copy(tenantId = resolveTenant(entity.tenantId))
            val now = java.time.Instant.now().toString()
            val savedId = dao.save(owned.toEntity(now, identity = identityFor(owned.id)))
            val saved = owned.copy(id = if (owned.id == 0L) savedId else owned.id)
            cache.remove(cacheKeyAll)
            Result.Success(saved)
        } catch (e: Exception) {
            AppLogger.e("CrmPatientRepository", "save failed", e)
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun saveAll(entities: List<CrmPatient>): Result<Int> {
        return try {
            val now = java.time.Instant.now().toString()
            val owned = entities.map { it.copy(tenantId = resolveTenant(it.tenantId)) }
            dao.saveAll(owned.map { it.toEntity(now, identity = identityFor(it.id)) })
            cache.remove(cacheKeyAll)
            Result.Success(owned.size)
        } catch (e: Exception) {
            AppLogger.e("CrmPatientRepository", "saveAll failed", e)
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun stageCount(stage: String): Result<Int> {
        return try {
            val count = dao.countByStage(tenantId, stage)
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun getPendingSync(since: String): Result<List<CrmPatient>> {
        return try {
            val entities = dao.getChangedSince(tenantId, since)
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(AppError.SyncError(e))
        }
    }

    override suspend fun deleteById(id: Long): Result<Unit> {
        return try {
            dao.softDelete(id, tenantId, java.time.Instant.now().toString())
            cache.remove(cacheKeyAll)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    /**
     * Returns the tenant this row belongs to, stamping the current one when the
     * caller left it unset. See the equivalent note in AppointmentRepositoryImpl:
     * a UI has no business knowing tenant ids, so "unset" is a normal thing to
     * receive, not an error. Only a non-zero id belonging to a *different*
     * tenant is a real violation.
     */
    /**
     * The sync identity to write with: the existing row's when this is an edit,
     * a fresh one when it is a new record.
     *
     * Minting a new identity on every save instead would give the same patient a
     * different client id each time she is edited, and the server — which keys
     * on client id — would accumulate a new duplicate patient per edit.
     */
    private suspend fun identityFor(id: Long): com.bioacupunt.sync.SyncIdentity {
        if (id == 0L) return com.bioacupunt.sync.SyncIdentity.new()
        val existing = dao.getById(id, tenantId) ?: return com.bioacupunt.sync.SyncIdentity.new()
        return com.bioacupunt.sync.SyncIdentity.carryForward(
            clientId = existing.clientId,
            serverId = existing.serverId,
            baseRev = existing.baseRev,
        )
    }

    private fun resolveTenant(entityTenantId: Long): Long {
        val current = tenantManager.requireTenantId()
        if (entityTenantId != 0L && entityTenantId != current) {
            throw IllegalArgumentException(
                "Tenant mismatch on CRM patient operation: $entityTenantId != $current"
            )
        }
        return current
    }
}
