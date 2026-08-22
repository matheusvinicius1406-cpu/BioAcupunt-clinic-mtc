package com.bioacupunt.crm

import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.CrmActivityType
import com.bioacupunt.crm.domain.model.CrmIdentityMap
import com.bioacupunt.crm.domain.model.CrmLead
import com.bioacupunt.crm.domain.model.CrmOrganization
import com.bioacupunt.crm.domain.model.CrmPipeline
import com.bioacupunt.crm.domain.model.CrmPerson
import com.bioacupunt.crm.domain.model.CrmTag
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.LeadStatus
import com.bioacupunt.crm.domain.model.OrganizationType
import com.bioacupunt.crm.domain.model.Patient360
import com.bioacupunt.crm.domain.model.PatientOperationalStatus
import com.bioacupunt.crm.domain.model.PersonType
import com.bioacupunt.crm.domain.model.PipelineStage
import com.bioacupunt.crm.domain.model.TaskPriority
import com.bioacupunt.crm.domain.model.TaskRelation
import com.bioacupunt.crm.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrmExtendedModelsTest {

    @Test
    fun personType_allValuesExist() {
        assertEquals(6, PersonType.values().size)
        assertEquals("Paciente", PersonType.PATIENT.label)
        assertEquals("Profissional", PersonType.PROFESSIONAL.label)
        assertEquals("Referenciador", PersonType.REFERRER.label)
        assertEquals("Cuidador", PersonType.CAREGIVER.label)
        assertEquals("Contato", PersonType.CONTACT.label)
        assertEquals("Lead", PersonType.LEAD.label)
    }

    @Test
    fun crmPerson_defaultValues() {
        val person = CrmPerson(tenantId = 1L, personType = PersonType.PATIENT, name = "Maria")
        assertEquals(0L, person.id)
        assertEquals("Maria", person.name)
        assertTrue(person.phone.isEmpty())
        assertTrue(person.email.isEmpty())
        assertEquals("ACTIVE", person.status)
    }

    @Test
    fun organizationType_allValuesExist() {
        assertEquals(8, OrganizationType.values().size)
        assertEquals("Clínica", OrganizationType.CLINIC.label)
        assertEquals("Hospital", OrganizationType.HOSPITAL.label)
        assertEquals("Seguradora", OrganizationType.INSURANCE.label)
    }

    @Test
    fun crmOrganization_defaultValues() {
        val org = CrmOrganization(tenantId = 1L, name = "Clínica Central")
        assertEquals("Clínica Central", org.name)
        assertEquals(OrganizationType.OTHER, org.type)
        assertEquals("ACTIVE", org.status)
    }

    @Test
    fun leadStatus_allValuesExist() {
        assertEquals(7, LeadStatus.values().size)
        assertEquals("Novo", LeadStatus.NEW.label)
        assertEquals("Convertido", LeadStatus.CONVERTED.label)
        assertEquals("Perdido", LeadStatus.LOST.label)
    }

    @Test
    fun crmLead_defaultValues() {
        val lead = CrmLead(tenantId = 1L, name = "João")
        assertEquals("João", lead.name)
        assertEquals(LeadStatus.NEW, lead.status)
        assertNull(lead.convertedPatientId)
    }

    @Test
    fun crmPipeline_withStages() {
        val pipeline = CrmPipeline(tenantId = 1L, name = "Cuidado")
        val stages = listOf(
            PipelineStage(pipelineId = 1L, tenantId = 1L, name = "Novo", order = 0),
            PipelineStage(pipelineId = 1L, tenantId = 1L, name = "Ativo", order = 1),
        )
        assertEquals("Cuidado", pipeline.name)
        assertEquals(2, stages.size)
        assertEquals(0, stages[0].order)
        assertEquals(1, stages[1].order)
    }

    @Test
    fun taskStatus_allValuesExist() {
        assertEquals(5, TaskStatus.values().size)
        assertEquals("Pendente", TaskStatus.PENDING.label)
        assertEquals("Atrasada", TaskStatus.OVERDOWN.label)
    }

    @Test
    fun taskPriority_allValuesExist() {
        assertEquals(4, TaskPriority.values().size)
        assertEquals("Urgente", TaskPriority.URGENT.label)
    }

    @Test
    fun taskRelation_allValuesExist() {
        assertEquals(8, TaskRelation.values().size)
        assertEquals("Paciente", TaskRelation.PATIENT.label)
        assertEquals("Retorno", TaskRelation.FOLLOW_UP.label)
    }

    @Test
    fun crmTask_defaultValues() {
        val task = CrmTask(tenantId = 1L, title = "Ligar para paciente")
        assertEquals("Ligar para paciente", task.title)
        assertEquals(TaskStatus.PENDING, task.status)
        assertEquals(TaskPriority.MEDIUM, task.priority)
        assertEquals("ADMINISTRATIVE", task.category)
    }

    @Test
    fun activityType_allValuesExist() {
        assertEquals(11, CrmActivityType.values().size)
        assertEquals("Ligação", CrmActivityType.CALL.label)
        assertEquals("E-mail", CrmActivityType.EMAIL.label)
        assertEquals("Atendimento", CrmActivityType.ENCOUNTER.label)
    }

    @Test
    fun crmActivity_defaultValues() {
        val activity = CrmActivity(tenantId = 1L, type = CrmActivityType.CALL, title = "Ligação", timestamp = "2026-01-01")
        assertEquals(CrmActivityType.CALL, activity.type)
        assertEquals("Ligação", activity.title)
        assertNull(activity.durationMinutes)
    }

    @Test
    fun patient360_combinesClinicalAndCrm() {
        val p360 = Patient360(
            patientId = 10L,
            tenantId = 1L,
            name = "Maria",
            sessionCount = 5,
            lastEncounterDate = "2026-08-01",
            pendingTasks = 3,
            overdueTasks = 1,
            operationalStatus = PatientOperationalStatus.AT_RISK,
        )
        assertEquals(10L, p360.patientId)
        assertEquals(5, p360.sessionCount)
        assertEquals(3, p360.pendingTasks)
        assertEquals(PatientOperationalStatus.AT_RISK, p360.operationalStatus)
    }

    @Test
    fun patientOperationalStatus_allValues() {
        assertEquals(3, PatientOperationalStatus.values().size)
        assertEquals("Ativo", PatientOperationalStatus.ACTIVE.label)
        assertEquals("Em risco", PatientOperationalStatus.AT_RISK.label)
        assertEquals("Inativo", PatientOperationalStatus.INACTIVE.label)
    }

    @Test
    fun identityMap_linksEntities() {
        val map = CrmIdentityMap(
            tenantId = 1L,
            entityType = "PATIENT",
            crmEntityId = 100L,
            bioacupuntEntityId = 10L,
            bioacupuntEntityType = "Patient",
        )
        assertEquals(100L, map.crmEntityId)
        assertEquals(10L, map.bioacupuntEntityId)
    }

    @Test
    fun crmTag_storesColor() {
        val tag = CrmTag(tenantId = 1L, name = "VIP", color = "#FF0000")
        assertEquals("VIP", tag.name)
        assertEquals("#FF0000", tag.color)
    }
}
