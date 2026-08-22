package com.bioacupunt.crm

import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.TimelineEventType
import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.CrmActivityType
import com.bioacupunt.crm.domain.model.CrmPermission
import com.bioacupunt.crm.domain.model.CrmRole
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.TaskPriority
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.UnifiedTimelineSource
import com.bioacupunt.crm.domain.usecase.AccessResult
import com.bioacupunt.crm.domain.usecase.CrmAuditLogger
import com.bioacupunt.crm.domain.usecase.CrmPermissionChecker
import com.bioacupunt.crm.domain.usecase.UnifiedTimelineUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrmPhase7IntegrationTest {

    private val timelineUseCase = UnifiedTimelineUseCase()
    private val permissionChecker = CrmPermissionChecker()

    // ═════════════════════════════════════════════════════════════════════
    // UNIFIED TIMELINE
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun timeline_emptyInputs_returnsEmpty() {
        val result = timelineUseCase.buildTimeline(patientId = 1L, tenantId = 1L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun timeline_clinicalEvents_included() {
        val events = listOf(
            ClinicalTimelineEvent(
                id = "enc-1",
                patientId = 10L,
                tenantId = 1L,
                type = TimelineEventType.ENCOUNTER,
                date = "2026-08-20T10:00:00Z",
                title = "Sessão 1",
            )
        )
        val result = timelineUseCase.buildTimeline(
            patientId = 10L, tenantId = 1L, clinicalEvents = events
        )
        assertEquals(1, result.size)
        assertEquals(UnifiedTimelineSource.CLINICAL, result[0].source)
        assertEquals("Sessão 1", result[0].title)
    }

    @Test
    fun timeline_crmActivities_included() {
        val activities = listOf(
            CrmActivity(
                id = 1L,
                tenantId = 1L,
                type = CrmActivityType.CALL,
                title = "Ligação de retorno",
                timestamp = "2026-08-20T11:00:00Z",
                relatedEntityId = 10L,
            )
        )
        val result = timelineUseCase.buildTimeline(
            patientId = 10L, tenantId = 1L, crmActivities = activities
        )
        assertEquals(1, result.size)
        assertEquals(UnifiedTimelineSource.CRM, result[0].source)
    }

    @Test
    fun timeline_crmTasks_included() {
        val tasks = listOf(
            CrmTask(
                id = 1L,
                tenantId = 1L,
                title = "Enviar lembrete",
                status = TaskStatus.COMPLETED,
                relatedEntityId = 10L,
                createdAt = "2026-08-20T09:00:00Z",
            )
        )
        val result = timelineUseCase.buildTimeline(
            patientId = 10L, tenantId = 1L, crmTasks = tasks
        )
        assertEquals(1, result.size)
        assertEquals("TASK_COMPLETED", result[0].type)
    }

    @Test
    fun timeline_sortedByTimestampDescending() {
        val clinical = listOf(
            ClinicalTimelineEvent(id = "1", patientId = 10L, tenantId = 1L, type = TimelineEventType.ENCOUNTER, date = "2026-08-19T10:00:00Z", title = "Anterior"),
            ClinicalTimelineEvent(id = "2", patientId = 10L, tenantId = 1L, type = TimelineEventType.ENCOUNTER, date = "2026-08-20T10:00:00Z", title = "Mais recente"),
        )
        val result = timelineUseCase.buildTimeline(
            patientId = 10L, tenantId = 1L, clinicalEvents = clinical
        )
        assertEquals("Mais recente", result[0].title)
        assertEquals("Anterior", result[1].title)
    }

    @Test
    fun timeline_maxResults_limitsOutput() {
        val events = (1..20).map { i ->
            ClinicalTimelineEvent(
                id = "$i", patientId = 10L, tenantId = 1L,
                type = TimelineEventType.ENCOUNTER,
                date = "2026-08-${String.format("%02d", i)}T10:00:00Z",
                title = "Sessão $i",
            )
        }
        val result = timelineUseCase.buildTimeline(
            patientId = 10L, tenantId = 1L, clinicalEvents = events, maxResults = 5
        )
        assertEquals(5, result.size)
    }

    @Test
    fun timeline_deduplication_removesDuplicates() {
        val events = listOf(
            com.bioacupunt.crm.domain.model.UnifiedTimelineEvent(
                id = "a", patientId = 10L, tenantId = 1L,
                source = UnifiedTimelineSource.CLINICAL,
                type = "ENCOUNTER", title = "Test", timestamp = "2026-08-20T10:00:00Z",
                entityType = "ENCOUNTER", entityId = 1L,
            ),
            com.bioacupunt.crm.domain.model.UnifiedTimelineEvent(
                id = "b", patientId = 10L, tenantId = 1L,
                source = UnifiedTimelineSource.CRM,
                type = "ENCOUNTER", title = "Test", timestamp = "2026-08-20T10:00:00Z",
                entityType = "ENCOUNTER", entityId = 1L, // Same entity
            ),
        )
        val deduplicated = timelineUseCase.deduplicate(events)
        assertEquals(1, deduplicated.size)
    }

    // ═════════════════════════════════════════════════════════════════════
    // PERMISSION CHECKER
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun permission_owner_canDoEverything() {
        assertTrue(permissionChecker.hasPermission(CrmRole.OWNER, CrmPermission.VIEW_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.OWNER, CrmPermission.DELETE_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.OWNER, CrmPermission.MANAGE_USERS))
        assertTrue(permissionChecker.hasPermission(CrmRole.OWNER, CrmPermission.EXPORT_DATA))
    }

    @Test
    fun permission_readOnly_cannotEdit() {
        assertTrue(permissionChecker.hasPermission(CrmRole.READ_ONLY, CrmPermission.VIEW_PATIENT))
        assertFalse(permissionChecker.hasPermission(CrmRole.READ_ONLY, CrmPermission.EDIT_PATIENT))
        assertFalse(permissionChecker.hasPermission(CrmRole.READ_ONLY, CrmPermission.DELETE_PATIENT))
    }

    @Test
    fun permission_reception_cannotDelete() {
        assertTrue(permissionChecker.hasPermission(CrmRole.RECEPTION, CrmPermission.VIEW_PATIENT))
        assertFalse(permissionChecker.hasPermission(CrmRole.RECEPTION, CrmPermission.DELETE_PATIENT))
        assertFalse(permissionChecker.hasPermission(CrmRole.RECEPTION, CrmPermission.MANAGE_PIPELINES))
    }

    @Test
    fun permission_practitioner_canViewAndEdit() {
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.VIEW_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.EDIT_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.VIEW_CRM))
        assertFalse(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.MANAGE_USERS))
    }

    @Test
    fun verifyAccess_allowed() {
        val result = permissionChecker.verifyAccess(
            CrmRole.PRACTITIONER, CrmPermission.VIEW_PATIENT, "PATIENT", 10L
        )
        assertTrue(result.isAllowed)
        assertTrue(result is AccessResult.Allowed)
    }

    @Test
    fun verifyAccess_denied() {
        val result = permissionChecker.verifyAccess(
            CrmRole.READ_ONLY, CrmPermission.EDIT_PATIENT, "PATIENT", 10L
        )
        assertTrue(result.isDenied)
        assertTrue(result is AccessResult.Denied)
    }

    @Test
    fun permission_admin_canManageUsers() {
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.MANAGE_USERS))
    }

    @Test
    fun permission_billing_canExport() {
        assertTrue(permissionChecker.hasPermission(CrmRole.BILLING, CrmPermission.EXPORT_DATA))
    }

    // ═════════════════════════════════════════════════════════════════════
    // AUDIT LOGGER
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun auditLogger_recordsEvents() = runTest {
        val events = mutableListOf<com.bioacupunt.data.local.model.AuditTrailEntity>()
        val fakeDao = FakeAuditTrailDao { events.add(it) }
        val logger = CrmAuditLogger(fakeDao)

        logger.logPatientViewed(1L, "user-1", 10L)
        logger.logRecordCreated(1L, "user-1", "PERSON", 20L)
        logger.logTaskCompleted(1L, "user-1", 30L)

        assertEquals(3, events.size)
        assertEquals("CrmPatientViewed", events[0].action)
        assertEquals("CrmRecordCreated", events[1].action)
        assertEquals("CrmTaskCompleted", events[2].action)
    }

    @Test
    fun auditLogger_includesMetadata() = runTest {
        var capturedEvent: com.bioacupunt.data.local.model.AuditTrailEntity? = null
        val fakeDao = FakeAuditTrailDao { capturedEvent = it }
        val logger = CrmAuditLogger(fakeDao)

        logger.logLeadConverted(1L, "user-1", leadId = 5L, patientId = 10L)
        assertTrue(capturedEvent?.metadata?.contains("10") == true)
    }
}
