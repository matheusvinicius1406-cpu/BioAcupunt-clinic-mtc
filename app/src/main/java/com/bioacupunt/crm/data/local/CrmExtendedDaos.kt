package com.bioacupunt.crm.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// ═════════════════════════════════════════════════════════════════════
// Person DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmPersonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmPersonEntity): Long

    @Update
    suspend fun update(entity: CrmPersonEntity)

    @Query("SELECT * FROM crm_people WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmPersonEntity?

    @Query("SELECT * FROM crm_people WHERE tenantId = :tenantId AND deleted = 0 ORDER BY name ASC")
    suspend fun getAll(tenantId: Long): List<CrmPersonEntity>

    @Query("SELECT * FROM crm_people WHERE tenantId = :tenantId AND personType = :type AND deleted = 0 ORDER BY name ASC")
    suspend fun getByType(tenantId: Long, type: String): List<CrmPersonEntity>

    @Query("SELECT * FROM crm_people WHERE tenantId = :tenantId AND organizationId = :orgId AND deleted = 0 ORDER BY name ASC")
    suspend fun getByOrganization(tenantId: Long, orgId: Long): List<CrmPersonEntity>

    @Query("UPDATE crm_people SET deleted = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Organization DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmOrganizationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmOrganizationEntity): Long

    @Update
    suspend fun update(entity: CrmOrganizationEntity)

    @Query("SELECT * FROM crm_organizations WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmOrganizationEntity?

    @Query("SELECT * FROM crm_organizations WHERE tenantId = :tenantId AND deleted = 0 ORDER BY name ASC")
    suspend fun getAll(tenantId: Long): List<CrmOrganizationEntity>

    @Query("UPDATE crm_organizations SET deleted = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Lead DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmLeadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmLeadEntity): Long

    @Update
    suspend fun update(entity: CrmLeadEntity)

    @Query("SELECT * FROM crm_leads WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmLeadEntity?

    @Query("SELECT * FROM crm_leads WHERE tenantId = :tenantId AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getAll(tenantId: Long): List<CrmLeadEntity>

    @Query("SELECT * FROM crm_leads WHERE tenantId = :tenantId AND status = :status AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getByStatus(tenantId: Long, status: String): List<CrmLeadEntity>

    @Query("SELECT * FROM crm_leads WHERE tenantId = :tenantId AND pipelineId = :pipelineId AND deleted = 0 ORDER BY pipelineStageOrder ASC")
    suspend fun getByPipeline(tenantId: Long, pipelineId: Long): List<CrmLeadEntity>

    @Query("UPDATE crm_leads SET deleted = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Pipeline DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmPipelineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmPipelineEntity): Long

    @Update
    suspend fun update(entity: CrmPipelineEntity)

    @Query("SELECT * FROM crm_pipelines WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmPipelineEntity?

    @Query("SELECT * FROM crm_pipelines WHERE tenantId = :tenantId AND deleted = 0 ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(tenantId: Long): List<CrmPipelineEntity>

    @Query("SELECT * FROM crm_pipelines WHERE tenantId = :tenantId AND isDefault = 1 AND deleted = 0 LIMIT 1")
    suspend fun getDefault(tenantId: Long): CrmPipelineEntity?

    @Query("UPDATE crm_pipelines SET deleted = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Pipeline Stage DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface PipelineStageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PipelineStageEntity): Long

    @Query("SELECT * FROM crm_pipeline_stages WHERE pipelineId = :pipelineId AND tenantId = :tenantId ORDER BY `order` ASC")
    suspend fun getByPipeline(pipelineId: Long, tenantId: Long): List<PipelineStageEntity>

    @Query("DELETE FROM crm_pipeline_stages WHERE pipelineId = :pipelineId AND tenantId = :tenantId")
    suspend fun deleteByPipeline(pipelineId: Long, tenantId: Long)
}

// ═════════════════════════════════════════════════════════════════════
// Task DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmTaskEntity): Long

    @Update
    suspend fun update(entity: CrmTaskEntity)

    @Query("SELECT * FROM crm_tasks WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmTaskEntity?

    @Query("SELECT * FROM crm_tasks WHERE tenantId = :tenantId AND deleted = 0 ORDER BY dueDate ASC")
    suspend fun getAll(tenantId: Long): List<CrmTaskEntity>

    @Query("SELECT * FROM crm_tasks WHERE tenantId = :tenantId AND status = :status AND deleted = 0 ORDER BY dueDate ASC")
    suspend fun getByStatus(tenantId: Long, status: String): List<CrmTaskEntity>

    @Query("SELECT * FROM crm_tasks WHERE tenantId = :tenantId AND relationType = :relationType AND relatedEntityId = :entityId AND deleted = 0 ORDER BY dueDate ASC")
    suspend fun getByRelatedEntity(tenantId: Long, relationType: String, entityId: Long): List<CrmTaskEntity>

    @Query("SELECT * FROM crm_tasks WHERE tenantId = :tenantId AND dueDate != '' AND dueDate < :now AND status != 'COMPLETED' AND deleted = 0 ORDER BY dueDate ASC")
    suspend fun getOverdue(tenantId: Long, now: String): List<CrmTaskEntity>

    @Query("SELECT COUNT(*) FROM crm_tasks WHERE tenantId = :tenantId AND status != 'COMPLETED' AND deleted = 0")
    suspend fun countPending(tenantId: Long): Int

    @Query("UPDATE crm_tasks SET deleted = :deletedAt, updatedAt = :updatedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String, updatedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Activity DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmActivityEntity): Long

    @Query("SELECT * FROM crm_activities WHERE id = :id AND tenantId = :tenantId AND deleted = 0")
    suspend fun getById(id: Long, tenantId: Long): CrmActivityEntity?

    @Query("SELECT * FROM crm_activities WHERE tenantId = :tenantId AND deleted = 0 ORDER BY timestamp DESC")
    suspend fun getAll(tenantId: Long): List<CrmActivityEntity>

    @Query("SELECT * FROM crm_activities WHERE tenantId = :tenantId AND type = :type AND deleted = 0 ORDER BY timestamp DESC")
    suspend fun getByType(tenantId: Long, type: String): List<CrmActivityEntity>

    @Query("SELECT * FROM crm_activities WHERE tenantId = :tenantId AND relationType = :relationType AND relatedEntityId = :entityId AND deleted = 0 ORDER BY timestamp DESC")
    suspend fun getByRelatedEntity(tenantId: Long, relationType: String, entityId: Long): List<CrmActivityEntity>

    @Query("SELECT COUNT(*) FROM crm_activities WHERE tenantId = :tenantId AND deleted = 0")
    suspend fun countAll(tenantId: Long): Int

    @Query("UPDATE crm_activities SET deleted = :deletedAt WHERE id = :id AND tenantId = :tenantId")
    suspend fun softDelete(id: Long, tenantId: Long, deletedAt: String)
}

// ═════════════════════════════════════════════════════════════════════
// Tag DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmTagEntity): Long

    @Query("SELECT * FROM crm_tags WHERE tenantId = :tenantId ORDER BY name ASC")
    suspend fun getAll(tenantId: Long): List<CrmTagEntity>

    @Query("DELETE FROM crm_tags WHERE id = :id AND tenantId = :tenantId")
    suspend fun delete(id: Long, tenantId: Long)
}

// ═════════════════════════════════════════════════════════════════════
// Identity Map DAO
// ═════════════════════════════════════════════════════════════════════

@Dao
interface CrmIdentityMapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrmIdentityMapEntity): Long

    @Query("SELECT * FROM crm_identity_map WHERE tenantId = :tenantId AND crmEntityId = :crmId AND entityType = :entityType LIMIT 1")
    suspend fun getByCrmEntity(tenantId: Long, entityType: String, crmId: Long): CrmIdentityMapEntity?

    @Query("SELECT * FROM crm_identity_map WHERE tenantId = :tenantId AND bioacupuntEntityId = :bioId AND bioacupuntEntityType = :bioType LIMIT 1")
    suspend fun getByBioacupuntEntity(tenantId: Long, bioType: String, bioId: Long): CrmIdentityMapEntity?

    @Query("SELECT * FROM crm_identity_map WHERE tenantId = :tenantId")
    suspend fun getAll(tenantId: Long): List<CrmIdentityMapEntity>
}
