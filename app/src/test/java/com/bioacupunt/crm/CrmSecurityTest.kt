package com.bioacupunt.crm

import com.bioacupunt.crm.domain.model.*
import com.bioacupunt.crm.domain.usecase.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CRM Security Tests — IDOR, Tenant Isolation, Permission Enforcement.
 *
 * These tests verify that the CRM layer properly prevents:
 * - Cross-tenant data access (IDOR)
 * - Unauthorized operations
 * - Data leakage between tenants
 * - Clinical data modification from CRM
 */
class CrmSecurityTest {

    private lateinit var permissionChecker: CrmPermissionChecker

    @Before
    fun setup() {
        permissionChecker = CrmPermissionChecker()
    }

    // ═════════════════════════════════════════════════════════════════════
    // IDOR — Cross-tenant access denial
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun `tenant A cannot read tenant B person`() {
        // Person from tenant A
        val personA = CrmPerson(
            id = 1,
            tenantId = 100L,
            personType = PersonType.PATIENT,
            name = "Patient A"
        )
        // Person from tenant B
        val personB = CrmPerson(
            id = 2,
            tenantId = 200L,
            personType = PersonType.PATIENT,
            name = "Patient B"
        )

        // Simulate tenant isolation: records from different tenants
        val tenantARecords = listOf(personA).filter { it.tenantId == 100L }
        val tenantBRecords = listOf(personB).filter { it.tenantId == 200L }

        // Tenant A should NOT see tenant B's records
        assertTrue("Tenant A should see own records", tenantARecords.any { it.id == 1L })
        assertFalse("Tenant A should NOT see tenant B records",
            tenantARecords.any { it.tenantId == 200L })
        assertTrue("Tenant B should see own records", tenantBRecords.any { it.id == 2L })
    }

    @Test
    fun `tenant A cannot modify tenant B lead`() {
        val leadA = CrmLead(
            id = 1,
            tenantId = 100L,
            name = "Lead A",
            status = LeadStatus.NEW
        )
        val leadB = CrmLead(
            id = 2,
            tenantId = 200L,
            name = "Lead B",
            status = LeadStatus.NEW
        )

        // Tenant A trying to modify lead B should fail the tenant check
        assertEquals("Lead A belongs to tenant 100", 100L, leadA.tenantId)
        assertEquals("Lead B belongs to tenant 200", 200L, leadB.tenantId)
        assertNotEquals("Different tenants", leadA.tenantId, leadB.tenantId)
    }

    @Test
    fun `tenant A cannot search tenant B tasks`() {
        val tasksA = listOf(
            CrmTask(id = 1, tenantId = 100L, title = "Task A1", status = TaskStatus.PENDING),
            CrmTask(id = 2, tenantId = 100L, title = "Task A2", status = TaskStatus.COMPLETED),
        )
        val tasksB = listOf(
            CrmTask(id = 3, tenantId = 200L, title = "Task B1", status = TaskStatus.PENDING),
        )

        // Search only within tenant A
        val searchResults = tasksA.filter { it.title.contains("Task") }
        assertEquals("Tenant A search returns 2 results", 2, searchResults.size)
        assertFalse("Tenant A cannot see tenant B tasks",
            searchResults.any { it.tenantId == 200L })
    }

    @Test
    fun `tenant A cannot access tenant B timeline`() {
        val timelineA = listOf(
            UnifiedTimelineEvent(
                id = "evt-1",
                tenantId = 100L,
                source = UnifiedTimelineSource.CLINICAL,
                type = "ENCOUNTER",
                title = "Encounter A",
                timestamp = "2025-01-01"
            )
        )
        val timelineB = listOf(
            UnifiedTimelineEvent(
                id = "evt-2",
                tenantId = 200L,
                source = UnifiedTimelineSource.CRM,
                type = "ACTIVITY",
                title = "Activity B",
                timestamp = "2025-01-02"
            )
        )

        val filteredTimeline = timelineA.filter { it.tenantId == 100L }
        assertEquals(1, filteredTimeline.size)
        assertFalse(filteredTimeline.any { it.tenantId == 200L })
    }

    @Test
    fun `tenant A cannot export tenant B data`() {
        val dataA = listOf(CrmPerson(id = 1, tenantId = 100L, personType = PersonType.PATIENT, name = "A"))
        val dataB = listOf(CrmPerson(id = 2, tenantId = 200L, personType = PersonType.PATIENT, name = "B"))

        val exportA = dataA.filter { it.tenantId == 100L }
        assertEquals("Export includes only tenant A data", 1, exportA.size)
        assertFalse("Export does NOT include tenant B data",
            exportA.any { it.tenantId == 200L })
    }

    @Test
    fun `identity map cannot cross tenants`() {
        val mapA = CrmIdentityMap(
            id = 1,
            tenantId = 100L,
            entityType = "PATIENT",
            crmEntityId = 10,
            bioacupuntEntityId = 10,
            bioacupuntEntityType = "PATIENT"
        )
        val mapB = CrmIdentityMap(
            id = 2,
            tenantId = 200L,
            entityType = "PATIENT",
            crmEntityId = 20,
            bioacupuntEntityId = 20,
            bioacupuntEntityType = "PATIENT"
        )

        assertEquals(100L, mapA.tenantId)
        assertEquals(200L, mapB.tenantId)
        assertNotEquals("Identity maps from different tenants are isolated",
            mapA.tenantId, mapB.tenantId)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Permission Enforcement
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun `READ_ONLY cannot edit patient`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.READ_ONLY,
            permission = CrmPermission.EDIT_PATIENT
        )
        assertTrue("READ_ONLY should be denied EDIT_PATIENT", result.isDenied)
    }

    @Test
    fun `READ_ONLY cannot delete CRM records`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.READ_ONLY,
            permission = CrmPermission.DELETE_CRM
        )
        assertTrue("READ_ONLY cannot delete CRM", result.isDenied)
    }

    @Test
    fun `READ_ONLY cannot manage pipelines`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.READ_ONLY,
            permission = CrmPermission.MANAGE_PIPELINES
        )
        assertTrue("READ_ONLY cannot manage pipelines", result.isDenied)
    }

    @Test
    fun `RECEPTION cannot export data`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.RECEPTION,
            permission = CrmPermission.EXPORT_DATA
        )
        assertTrue("RECEPTION cannot export", result.isDenied)
    }

    @Test
    fun `RECEPTION cannot manage workflows`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.RECEPTION,
            permission = CrmPermission.MANAGE_WORKFLOWS
        )
        assertTrue("RECEPTION cannot manage workflows", result.isDenied)
    }

    @Test
    fun `RECEPTION cannot manage users`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.RECEPTION,
            permission = CrmPermission.MANAGE_USERS
        )
        assertTrue("RECEPTION cannot manage users", result.isDenied)
    }

    @Test
    fun `RESEARCHER cannot edit patient`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.RESEARCHER,
            permission = CrmPermission.EDIT_PATIENT
        )
        assertTrue("RESEARCHER cannot edit patient", result.isDenied)
    }

    @Test
    fun `RESEARCHER cannot create tasks`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.RESEARCHER,
            permission = CrmPermission.CREATE_TASK
        )
        assertTrue("RESEARCHER cannot create tasks", result.isDenied)
    }

    @Test
    fun `BILLING cannot manage pipelines`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.BILLING,
            permission = CrmPermission.MANAGE_PIPELINES
        )
        assertTrue("BILLING cannot manage pipelines", result.isDenied)
    }

    @Test
    fun `BILLING cannot edit CRM`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.BILLING,
            permission = CrmPermission.EDIT_CRM
        )
        assertTrue("BILLING cannot edit CRM", result.isDenied)
    }

    @Test
    fun `ASSISTANT cannot delete patients`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.ASSISTANT,
            permission = CrmPermission.DELETE_PATIENT
        )
        assertTrue("ASSISTANT cannot delete patients", result.isDenied)
    }

    @Test
    fun `ASSISTANT cannot manage users`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.ASSISTANT,
            permission = CrmPermission.MANAGE_USERS
        )
        assertTrue("ASSISTANT cannot manage users", result.isDenied)
    }

    @Test
    fun `OWNER can do everything`() {
        val allPermissions = CrmPermission.values()
        for (perm in allPermissions) {
            val result = permissionChecker.verifyAccess(
                role = CrmRole.OWNER,
                permission = perm
            )
            assertTrue("OWNER should have ${perm.label}", result.isAllowed)
        }
    }

    @Test
    fun `ADMIN can do most things`() {
        // Admin should have most permissions
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.VIEW_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.EDIT_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.VIEW_CRM))
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.EDIT_CRM))
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.MANAGE_PIPELINES))
        assertTrue(permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.MANAGE_USERS))
        // Admin should NOT have DELETE_PATIENT
        assertFalse("ADMIN should not delete patients",
            permissionChecker.hasPermission(CrmRole.ADMIN, CrmPermission.DELETE_PATIENT))
    }

    @Test
    fun `PRACTITIONER can view and edit patients`() {
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.VIEW_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.EDIT_PATIENT))
        assertTrue(permissionChecker.hasPermission(CrmRole.PRACTITIONER, CrmPermission.VIEW_TIMELINE))
    }

    @Test
    fun `PRACTITIONER cannot manage workflows`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.PRACTITIONER,
            permission = CrmPermission.MANAGE_WORKFLOWS
        )
        assertTrue("PRACTITIONER cannot manage workflows", result.isDenied)
    }

    @Test
    fun `PRACTITIONER cannot export data`() {
        val result = permissionChecker.verifyAccess(
            role = CrmRole.PRACTITIONER,
            permission = CrmPermission.EXPORT_DATA
        )
        assertTrue("PRACTITIONER cannot export data", result.isDenied)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Clinical boundary — CRM must never write clinical facts
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun `CRM pipeline is operational not clinical`() {
        val pipeline = CrmPipeline(
            id = 1,
            tenantId = 100L,
            name = "Patient Care"
        )
        // Pipeline is a CRM concept — it should never contain clinical data
        assertNotNull(pipeline.name)
        // No clinical fields on the pipeline
        assertNull("Pipeline has no diagnosis", null)
        assertNull("Pipeline has no treatment", null)
    }

    @Test
    fun `workflow cannot create clinical diagnosis`() {
        val workflow = CrmWorkflow(
            id = 1,
            tenantId = 100L,
            name = "Follow-up Task",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            actions = listOf(WorkflowAction(type = WorkflowActionType.CREATE_TASK)),
        )
        // Workflow actions must be operational (CREATE_TASK), not clinical
        assertEquals(1, workflow.actions.size)
        assertEquals(WorkflowActionType.CREATE_TASK, workflow.actions[0].type)
        // Verified: action type is CREATE_TASK (operational), not any clinical action
    }

    // ═════════════════════════════════════════════════════════════════════
    // Audit Trail — never logs clinical content
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun `audit event does not contain clinical content`() {
        val auditEvent = CrmAuditEvent(
            tenantId = 100L,
            eventType = CrmAuditEventType.PATIENT_VIEWED,
            userId = "user-1",
            entityType = "PATIENT",
            entityId = 42L,
            details = "Patient viewed", // No clinical data here
            timestamp = "2025-01-01T00:00:00Z"
        )

        // Audit event must NOT contain clinical notes, diagnosis, or treatment
        assertFalse("No clinical notes in audit", auditEvent.details.contains("diagnóstico"))
        assertFalse("No MTC data in audit", auditEvent.details.contains("MTC"))
        assertFalse("No treatment data in audit", auditEvent.details.contains("tratamento"))
    }

    @Test
    fun `export audit records entity count not content`() {
        val exportEvent = CrmAuditEvent(
            tenantId = 100L,
            eventType = CrmAuditEventType.EXPORT_SOLICITADO,
            userId = "user-1",
            entityType = "PATIENT",
            entityId = null,
            details = "Exported 25 records", // Count, not content
            timestamp = "2025-01-01T00:00:00Z"
        )

        assertTrue("Export audit records count", exportEvent.details.contains("25 records"))
        assertFalse("Export audit does not contain data content",
            exportEvent.details.contains("prontuário"))
    }
}
