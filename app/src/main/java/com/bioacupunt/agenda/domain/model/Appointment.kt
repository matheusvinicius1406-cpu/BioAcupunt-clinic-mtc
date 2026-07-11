package com.bioacupunt.agenda.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: Long = 0L,
    val tenantId: Long = 0L,
    val patientId: Long = 0L,
    val patientName: String = "",
    val date: String = "",      // ISO date: yyyy-MM-dd
    val time: String = "",      // HH:mm
    val durationMin: Int = 60,
    val type: String = AppointmentType.ACUPUNCTURE.name,
    val status: String = AppointmentStatus.SCHEDULED.name,
    val notes: String = "",
    val sessionNumber: Int = 1,
    val reminderMinutesBefore: Int = 30,
    val valueBrl: Double = 0.0,
    val paid: Boolean = false,
    val createdAt: String = ""
)