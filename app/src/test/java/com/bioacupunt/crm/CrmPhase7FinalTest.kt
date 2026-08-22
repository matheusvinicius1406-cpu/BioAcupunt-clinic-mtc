package com.bioacupunt.crm

import com.bioacupunt.crm.adapter.TwentyApiClient
import com.bioacupunt.crm.adapter.TwentyMappers
import com.bioacupunt.crm.domain.model.CrmOrganization
import com.bioacupunt.crm.domain.model.CrmPerson
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.CrmWorkflow
import com.bioacupunt.crm.domain.model.OrganizationType
import com.bioacupunt.crm.domain.model.PersonType
import com.bioacupunt.crm.domain.model.Referral
import com.bioacupunt.crm.domain.model.ReferralStatus
import com.bioacupunt.crm.domain.model.TaskPriority
import com.bioacupunt.crm.domain.model.TaskRelation
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.WorkflowAction
import com.bioacupunt.crm.domain.model.WorkflowActionType
import com.bioacupunt.crm.domain.model.WorkflowCondition
import com.bioacupunt.crm.domain.model.WorkflowTriggerType
import com.bioacupunt.crm.domain.usecase.CrmAuditLogger
import com.bioacupunt.crm.domain.usecase.ExecutionResult
import com.bioacupunt.crm.domain.usecase.ReferralUseCase
import com.bioacupunt.crm.domain.usecase.WorkflowContext
import com.bioacupunt.crm.domain.usecase.WorkflowExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrmPhase7FinalTest {

    // ═════════════════════════════════════════════════════════════════════
    // TWENTY MAPPER
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun mapper_personToTwenty() {
        val person = CrmPerson(
            tenantId = 1L,
            personType = PersonType.PATIENT,
            name = "Maria Silva",
            phone = "11999999999",
            email = "maria@test.com",
        )
        val twenty = TwentyMappers.personToTwenty(person)
        assertEquals("Maria", twenty["firstName"])
        assertEquals("Silva", twenty["lastName"])
        assertEquals("11999999999", twenty["phone"])
        assertEquals("maria@test.com", twenty["email"])
    }

    @Test
    fun mapper_organizationToTwenty() {
        val org = CrmOrganization(
            tenantId = 1L,
            name = "Clínica Central",
            website = "https://clinica.com",
            address = "Rua A, 123",
        )
        val twenty = TwentyMappers.organizationToTwenty(org)
        assertEquals("Clínica Central", twenty["name"])
        assertEquals("https://clinica.com", twenty["domainName"])
        assertEquals("Rua A, 123", twenty["address"])
    }

    @Test
    fun mapper_twentyToPerson() {
        val record = TwentyApiClient.TwentyRecord(
            id = "42",
            objectMetadataId = "person",
            fields = mapOf(
                "firstName" to "João",
                "lastName" to "Santos",
                "phone" to "11888888888",
                "email" to "joao@test.com",
                "personType" to "PATIENT",
            ),
            createdAt = "2026-01-01",
            updatedAt = "2026-01-01",
        )
        val person = TwentyMappers.twentyToPerson(record)
        assertEquals("João Santos", person.name)
        assertEquals(PersonType.PATIENT, person.personType)
    }

    @Test
    fun mapper_twentyToOrganization() {
        val record = TwentyApiClient.TwentyRecord(
            id = "99",
            objectMetadataId = "company",
            fields = mapOf(
                "name" to "Hospital Central",
                "organizationType" to "HOSPITAL",
            ),
        )
        val org = TwentyMappers.twentyToOrganization(record)
        assertEquals("Hospital Central", org.name)
        assertEquals(OrganizationType.HOSPITAL, org.type)
    }

    // ═════════════════════════════════════════════════════════════════════
    // TWENTY API CLIENT — construction and error handling
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun twentyClient_constructsWithValidParams() {
        val client = TwentyApiClient(
            baseUrl = "https://twenty.example.com",
            token = "test-token",
        )
        assertNotNull(client)
    }

    @Test
    fun twentyClient_healthCheckFailsGracefully() = runTest {
        val client = TwentyApiClient(
            baseUrl = "https://twenty.invalid.example.com",
            token = "test-token",
        )
        val result = client.healthCheck()
        // Connection to invalid host should fail gracefully, not crash
        assertFalse("Health check should fail for invalid host", result.success)
        assertNotNull("Error message should be present", result.error)
    }

    // ═════════════════════════════════════════════════════════════════════
    // WORKFLOW EXECUTOR
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun workflowExecutor_createsTask() = runTest {
        val createdTasks = mutableListOf<CrmTask>()
        val executor = WorkflowExecutor(
            taskRepository = object : WorkflowExecutor.TaskRepository {
                override suspend fun create(task: CrmTask): Long {
                    createdTasks.add(task)
                    return createdTasks.size.toLong()
                }
            },
            auditLogger = createAuditLogger(),
        )

        val workflow = CrmWorkflow(
            id = 1L,
            tenantId = 1L,
            name = "Follow-up automático",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            actions = listOf(
                WorkflowAction(
                    type = WorkflowActionType.CREATE_TASK,
                    params = mapOf("title" to "Follow-up pendente", "priority" to "HIGH"),
                )
            ),
        )

        val result = executor.execute(
            workflow = workflow,
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            context = WorkflowContext(patientId = 10L),
        )

        assertTrue(result.success)
        assertEquals(1, result.createdTaskIds.size)
        assertEquals(1, createdTasks.size)
        assertEquals("Follow-up pendente", createdTasks[0].title)
        assertEquals(TaskPriority.HIGH, createdTasks[0].priority)
    }

    @Test
    fun workflowExecutor_rejectsClinicalAction() = runTest {
        val executor = WorkflowExecutor(
            taskRepository = object : WorkflowExecutor.TaskRepository {
                override suspend fun create(task: CrmTask): Long = 1L
            },
            auditLogger = createAuditLogger(),
        )

        val workflow = CrmWorkflow(
            id = 1L,
            tenantId = 1L,
            name = "Illegal workflow",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            actions = listOf(
                WorkflowAction(
                    type = WorkflowActionType.CREATE_TASK,
                    params = mapOf("CREATE_DIAGNOSIS" to "true"),
                )
            ),
        )

        val result = executor.execute(
            workflow = workflow,
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            context = WorkflowContext(patientId = 10L),
        )

        assertFalse(result.success)
        assertTrue(result.reason.contains("Clinical action blocked"))
    }

    @Test
    fun workflowExecutor_conditionsNotMet() = runTest {
        val executor = WorkflowExecutor(
            taskRepository = object : WorkflowExecutor.TaskRepository {
                override suspend fun create(task: CrmTask): Long = 1L
            },
            auditLogger = createAuditLogger(),
        )

        val workflow = CrmWorkflow(
            id = 1L,
            tenantId = 1L,
            name = "Conditional workflow",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            conditions = listOf(
                WorkflowCondition(field = "followUpRequired", operator = "equals", value = "true")
            ),
            actions = listOf(
                WorkflowAction(type = WorkflowActionType.CREATE_TASK, params = mapOf("title" to "Task"))
            ),
        )

        val result = executor.execute(
            workflow = workflow,
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
            context = WorkflowContext(patientId = 10L, data = mapOf("followUpRequired" to "false")),
        )

        assertFalse(result.success)
        assertTrue(result.reason.contains("Conditions not met"))
    }

    @Test
    fun workflowExecutor_triggerMismatch() = runTest {
        val executor = WorkflowExecutor(
            taskRepository = object : WorkflowExecutor.TaskRepository {
                override suspend fun create(task: CrmTask): Long = 1L
            },
            auditLogger = createAuditLogger(),
        )

        val workflow = CrmWorkflow(
            id = 1L,
            tenantId = 1L,
            name = "Wrong trigger",
            triggerType = WorkflowTriggerType.ENCOUNTER_COMPLETED,
        )

        val result = executor.execute(
            workflow = workflow,
            triggerType = WorkflowTriggerType.LEAD_QUALIFIED,
            context = WorkflowContext(patientId = 10L),
        )

        assertFalse(result.success)
        assertTrue(result.reason.contains("Trigger mismatch"))
    }

    // ═════════════════════════════════════════════════════════════════════
    // REFERRAL USE CASE
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun referralUseCase_createReferral() = runTest {
        val savedReferrals = mutableListOf<Referral>()
        val useCase = ReferralUseCase(
            referralRepository = object : ReferralUseCase.ReferralRepository {
                override suspend fun save(referral: Referral): Long {
                    savedReferrals.add(referral)
                    return savedReferrals.size.toLong()
                }
                override suspend fun getById(id: Long) = savedReferrals.getOrNull((id - 1).toInt())
                override suspend fun getByPatientId(patientId: Long) = emptyList<Referral>()
                override suspend fun updateStatus(id: Long, status: ReferralStatus) {}
            },
            leadRepository = object : ReferralUseCase.LeadRepository {
                override suspend fun save(lead: com.bioacupunt.crm.domain.model.CrmLead) = 1L
            },
            auditLogger = createAuditLogger(),
        )

        val referral = useCase.createReferral(
            tenantId = 1L,
            reason = "Dor lombar crônica",
            notes = "Encaminhado pela Dra. Ana",
        )

        assertEquals(1, referral.id)
        assertEquals("Dor lombar crônica", referral.reason)
        assertEquals(ReferralStatus.PENDING, referral.status)
    }

    @Test
    fun referralUseCase_convertToLead() = runTest {
        var updatedStatus: ReferralStatus? = null
        var savedLead: com.bioacupunt.crm.domain.model.CrmLead? = null

        val useCase = ReferralUseCase(
            referralRepository = object : ReferralUseCase.ReferralRepository {
                override suspend fun save(referral: Referral) = 1L
                override suspend fun getById(id: Long) = Referral(
                    id = 1L, tenantId = 1L, reason = "Dor", status = ReferralStatus.PENDING
                )
                override suspend fun getByPatientId(patientId: Long) = emptyList<Referral>()
                override suspend fun updateStatus(id: Long, status: ReferralStatus) {
                    updatedStatus = status
                }
            },
            leadRepository = object : ReferralUseCase.LeadRepository {
                override suspend fun save(lead: com.bioacupunt.crm.domain.model.CrmLead): Long {
                    savedLead = lead
                    return 1L
                }
            },
            auditLogger = createAuditLogger(),
        )

        val lead = useCase.convertToLead(referralId = 1L, tenantId = 1L)

        assertNotNull(lead)
        assertEquals(ReferralStatus.CONTACTED, updatedStatus)
        assertEquals("REFERRAL", savedLead?.source)
    }

    // ═════════════════════════════════════════════════════════════════════
    // AUDIT LOGGER
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun auditLogger_recordsAllEvents() = runTest {
        val events = mutableListOf<com.bioacupunt.data.local.model.AuditTrailEntity>()
        val fakeDao = FakeAuditTrailDao { events.add(it) }
        val logger = CrmAuditLogger(fakeDao)

        logger.logPatientViewed(1L, "user", 10L)
        logger.logRecordCreated(1L, "user", "PERSON", 20L)
        logger.logLeadConverted(1L, "user", 5L, 10L)
        logger.logTaskCreated(1L, "user", 30L)
        logger.logTaskCompleted(1L, "user", 30L)
        logger.logActivityCreated(1L, "user", 40L)
        logger.logReferralCreated(1L, "user", 50L)
        logger.logWorkflowExecuted(1L, "user", 60L, "ENCOUNTER_COMPLETED")
        logger.logViewCreated(1L, "user", 70L)
        logger.logExportRequested(1L, "user", "PATIENT", 10)
        logger.logSearchPerformed(1L, "user", "Maria", 5)

        assertEquals(11, events.size)
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPER
    // ═════════════════════════════════════════════════════════════════════

    private fun createAuditLogger(): CrmAuditLogger {
        return CrmAuditLogger(FakeAuditTrailDao { })
    }
}
