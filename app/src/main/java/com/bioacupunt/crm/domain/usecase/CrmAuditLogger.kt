package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.data.local.database.AuditTrailDao
import com.bioacupunt.data.local.model.AuditTrailEntity
import java.util.UUID

/**
 * CRM Audit Logger — WRAPS existing AuditTrailEntity/AuditTrailDao.
 *
 * WHY_NEW_COMPONENT: The existing AuditTrailDao handles knowledge/ingestion events.
 * CRM needs to log people/pipeline/task/lead events using the SAME append-only
 * infrastructure. Rather than creating a parallel audit system, we extend the
 * existing one with CRM-specific action constants.
 *
 * Reuses:
 * - AuditTrailEntity (existing — append-only, LGPD compliant, tenant-scoped)
 * - AuditTrailDao (existing — insert, queries, anonymization, retention)
 *
 * Does NOT create:
 * - CrmAuditEvent entity (redundant — use AuditTrailEntity)
 * - CrmAuditEventDao (redundant — use AuditTrailDao)
 * - New audit table (redundant — use existing audit_trail table)
 */
class CrmAuditLogger(
    private val auditDao: AuditTrailDao,
) {

    // ═════════════════════════════════════════════════════════════════════
    // CRM-specific action constants (added to existing AuditTrailEntity)
    // ═════════════════════════════════════════════════════════════════════

    companion object {
        const val ACTION_PATIENT_VIEWED = "CrmPatientViewed"
        const val ACTION_RECORD_CREATED = "CrmRecordCreated"
        const val ACTION_RECORD_UPDATED = "CrmRecordUpdated"
        const val ACTION_RECORD_DELETED = "CrmRecordDeleted"
        const val ACTION_LEAD_CONVERTED = "CrmLeadConverted"
        const val ACTION_OPPORTUNITY_CHANGED = "CrmOpportunityChanged"
        const val ACTION_PIPELINE_CHANGED = "CrmPipelineChanged"
        const val ACTION_TASK_CREATED = "CrmTaskCreated"
        const val ACTION_TASK_COMPLETED = "CrmTaskCompleted"
        const val ACTION_ACTIVITY_CREATED = "CrmActivityCreated"
        const val ACTION_REFERRAL_CREATED = "CrmReferralCreated"
        const val ACTION_NOTE_CREATED = "CrmNoteCreated"
        const val ACTION_WORKFLOW_EXECUTED = "CrmWorkflowExecuted"
        const val ACTION_VIEW_CREATED = "CrmViewCreated"
        const val ACTION_EXPORT_REQUESTED = "CrmExportRequested"
        const val ACTION_SEARCH_PERFORMED = "CrmSearchPerformed"
        const val ACTION_CONFLICT_RESOLVED = "CrmConflictResolved"
        const val ACTION_IDENTITY_MAP_CREATED = "CrmIdentityMapCreated"
    }

    // ═════════════════════════════════════════════════════════════════════
    // Logging methods — all write to existing AuditTrailDao
    // tenantId is Long (CRM domain) → converted to String for AuditTrailEntity
    // ═════════════════════════════════════════════════════════════════════

    private fun Long.toTenantIdString(): String = this.toString()

    suspend fun logPatientViewed(tenantId: Long, userId: String, patientId: Long) {
        insert(ACTION_PATIENT_VIEWED, tenantId.toTenantIdString(), userId, "PATIENT", patientId.toString())
    }

    suspend fun logRecordCreated(tenantId: Long, userId: String, entityType: String, entityId: Long) {
        insert(ACTION_RECORD_CREATED, tenantId.toTenantIdString(), userId, entityType, entityId.toString())
    }

    suspend fun logRecordUpdated(tenantId: Long, userId: String, entityType: String, entityId: Long) {
        insert(ACTION_RECORD_UPDATED, tenantId.toTenantIdString(), userId, entityType, entityId.toString())
    }

    suspend fun logRecordDeleted(tenantId: Long, userId: String, entityType: String, entityId: Long) {
        insert(ACTION_RECORD_DELETED, tenantId.toTenantIdString(), userId, entityType, entityId.toString())
    }

    suspend fun logLeadConverted(tenantId: Long, userId: String, leadId: Long, patientId: Long) {
        insert(ACTION_LEAD_CONVERTED, tenantId.toTenantIdString(), userId, "LEAD", leadId.toString(),
            metadata = """{"convertedToPatientId":$patientId}""")
    }

    suspend fun logPipelineChanged(tenantId: Long, userId: String, pipelineId: Long, fromStage: String, toStage: String) {
        insert(ACTION_PIPELINE_CHANGED, tenantId.toTenantIdString(), userId, "PIPELINE", pipelineId.toString(),
            metadata = """{"fromStage":"$fromStage","toStage":"$toStage"}""")
    }

    suspend fun logTaskCreated(tenantId: Long, userId: String, taskId: Long) {
        insert(ACTION_TASK_CREATED, tenantId.toTenantIdString(), userId, "TASK", taskId.toString())
    }

    suspend fun logTaskCompleted(tenantId: Long, userId: String, taskId: Long) {
        insert(ACTION_TASK_COMPLETED, tenantId.toTenantIdString(), userId, "TASK", taskId.toString())
    }

    suspend fun logActivityCreated(tenantId: Long, userId: String, activityId: Long) {
        insert(ACTION_ACTIVITY_CREATED, tenantId.toTenantIdString(), userId, "ACTIVITY", activityId.toString())
    }

    suspend fun logReferralCreated(tenantId: Long, userId: String, referralId: Long) {
        insert(ACTION_REFERRAL_CREATED, tenantId.toTenantIdString(), userId, "REFERRAL", referralId.toString())
    }

    suspend fun logWorkflowExecuted(tenantId: Long, userId: String, workflowId: Long, triggerType: String) {
        insert(ACTION_WORKFLOW_EXECUTED, tenantId.toTenantIdString(), userId, "WORKFLOW", workflowId.toString(),
            metadata = """{"triggerType":"$triggerType"}""")
    }

    suspend fun logViewCreated(tenantId: Long, userId: String, viewId: Long) {
        insert(ACTION_VIEW_CREATED, tenantId.toTenantIdString(), userId, "VIEW", viewId.toString())
    }

    suspend fun logExportRequested(tenantId: Long, userId: String, entityType: String, entityCount: Int) {
        insert(ACTION_EXPORT_REQUESTED, tenantId.toTenantIdString(), userId, entityType, null,
            metadata = """{"entityCount":$entityCount}""")
    }

    suspend fun logSearchPerformed(tenantId: Long, userId: String, query: String, resultCount: Int) {
        insert(ACTION_SEARCH_PERFORMED, tenantId.toTenantIdString(), userId, "SEARCH", null,
            metadata = """{"query":"${query.take(50)}","resultCount":$resultCount}""")
    }

    suspend fun logConflictResolved(tenantId: Long, userId: String, entityType: String, entityId: Long, resolution: String) {
        insert(ACTION_CONFLICT_RESOLVED, tenantId.toTenantIdString(), userId, entityType, entityId.toString(),
            metadata = """{"resolution":"$resolution"}""")
    }

    suspend fun logIdentityMapCreated(tenantId: Long, userId: String, mappingId: Long) {
        insert(ACTION_IDENTITY_MAP_CREATED, tenantId.toTenantIdString(), userId, "IDENTITY_MAP", mappingId.toString())
    }

    // ═════════════════════════════════════════════════════════════════════
    // Internal — writes to existing AuditTrailEntity
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun insert(
        action: String,
        tenantId: String,
        userId: String,
        resourceType: String,
        resourceId: String?,
        metadata: String = "{}",
    ) {
        val event = AuditTrailEntity(
            id = UUID.randomUUID().toString(),
            tenant_id = tenantId,
            actor_id = userId,
            action = action,
            resource_type = resourceType,
            resource_id = resourceId,
            outcome = "success",
            metadata = metadata,
        )
        auditDao.insert(event)
    }
}
