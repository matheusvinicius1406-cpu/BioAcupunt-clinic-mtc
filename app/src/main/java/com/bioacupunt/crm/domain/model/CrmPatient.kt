package com.bioacupunt.crm.domain.model

data class CrmPatient(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val birthDate: String,
    val stage: String,
    val totalSessions: Int,
    val totalRevenueBrl: Double,
    val lastVisit: String,
    val nextAppointment: String,
    val tags: List<String>,
    val notes: String = "",
    val referralSource: String = "",
    val npsScore: Int? = null,
    val healthInsurance: String = "",
    val mainComplaint: String = "",
    val createdAt: String
)
