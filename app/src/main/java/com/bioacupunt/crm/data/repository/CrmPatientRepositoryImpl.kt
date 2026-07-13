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
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun save(entity: CrmPatient): Result<CrmPatient> {
        return try {
            validateTenant(entity.tenantId)
            val now = java.time.Instant.now().toString()
            val savedId = dao.save(entity.toEntity(now))
            val saved = entity.copy(id = if (entity.id == 0L) savedId else entity.id)
            cache.remove(cacheKeyAll)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun saveAll(entities: List<CrmPatient>): Result<Int> {
        return try {
            entities.forEach { validateTenant(it.tenantId) }
            val now = java.time.Instant.now().toString()
            dao.saveAll(entities.map { it.toEntity(now) })
            cache.remove(cacheKeyAll)
            Result.Success(entities.size)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun stageCount(stage: String): Result<Int> {
        return try {
            val count = dao.countByStage(tenantId, stage)
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
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
            Result.Error(AppError.DatabaseError(e))
        }
    }

    private fun validateTenant(entityTenantId: Long) {
        val current = tenantManager.currentTenantId()
        if (entityTenantId != current) {
            throw IllegalArgumentException("Tenant mismatch on CRM patient operation")
        }
    }
}
