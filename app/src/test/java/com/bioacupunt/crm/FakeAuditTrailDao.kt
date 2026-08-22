package com.bioacupunt.crm

import com.bioacupunt.data.local.database.AuditTrailDao
import com.bioacupunt.data.local.model.AuditTrailEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake AuditTrailDao for testing — captures inserted events.
 * Reuses the real AuditTrailEntity from the existing audit system.
 * Shared across all CRM test files in this package.
 */
class FakeAuditTrailDao(
    private val onInsert: (AuditTrailEntity) -> Unit
) : AuditTrailDao {
    override suspend fun insert(entry: AuditTrailEntity) { onInsert(entry) }
    override suspend fun insertAll(entries: List<AuditTrailEntity>) { entries.forEach(onInsert) }
    override suspend fun getById(id: String): AuditTrailEntity? = null
    override fun getByTenant(tenantId: String): Flow<List<AuditTrailEntity>> = flowOf(emptyList())
    override suspend fun getRecentByTenant(tenantId: String, limit: Int): List<AuditTrailEntity> = emptyList()
    override fun getByResource(resourceType: String, resourceId: String): Flow<List<AuditTrailEntity>> = flowOf(emptyList())
    override suspend fun getByAction(action: String, limit: Int): List<AuditTrailEntity> = emptyList()
    override suspend fun getByActor(actorId: String, limit: Int): List<AuditTrailEntity> = emptyList()
    override suspend fun getSince(since: Long): List<AuditTrailEntity> = emptyList()
    override suspend fun getByDateRange(tenantId: String, since: Long, until: Long): List<AuditTrailEntity> = emptyList()
    override suspend fun anonymizeForPurge(resourceId: String, actorId: String?, sentinel: String) {}
    override suspend fun countNotAnonymized(resourceId: String): Int = 0
    override suspend fun count(): Int = 0
    override suspend fun countByTenant(tenantId: String): Int = 0
    override suspend fun countByAction(action: String): Int = 0
    override suspend fun countSince(since: Long): Int = 0
    override suspend fun deleteOlderThan(threshold: Long): Int = 0
}
