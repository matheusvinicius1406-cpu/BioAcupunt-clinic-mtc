package com.bioacupunt.crm.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CrmPatient(
    val id: Long = 0L,
    val tenantId: Long = 0L,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val birthDate: String = "",
    val stage: String = PatientStage.FIRST_CONTACT.name,
    val totalSessions: Int = 0,
    val totalRevenueBrl: Double = 0.0,
    val lastVisit: String = "",
    val nextAppointment: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val referralSource: String = "",
    val npsScore: Int? = null,
    val healthInsurance: String = "",
    val mainComplaint: String = "",
    val createdAt: String = ""
)

@Serializable
data class CrmNote(
    val id: Long = 0L,
    val patientId: Long,
    val content: String,
    val type: String = "general",   // general, followup, alert
    val createdAt: String = ""
)

data class CrmStats(
    val totalPatients: Int,
    val activePatients: Int,
    val newThisMonth: Int,
    val churnedThisMonth: Int,
    val avgSessionsPerPatient: Double,
    val avgRevenue: Double,
    val retentionRate: Double
)
