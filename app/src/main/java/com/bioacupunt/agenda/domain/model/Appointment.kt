package com.bioacupunt.agenda.domain.model

import kotlinx.serialization.Serializable

enum class AppointmentStatus(val label: String, val color: Long) {
    SCHEDULED("Agendado", 0xFF64B5F6),
    CONFIRMED("Confirmado", 0xFF87B344),
    IN_PROGRESS("Em Atendimento", 0xFFFFB300),
    COMPLETED("Concluído", 0xFF4CAF50),
    CANCELLED("Cancelado", 0xFFEF5350),
    NO_SHOW("Faltou", 0xFFFF7043)
}

enum class AppointmentType(val label: String, val durationMin: Int) {
    FIRST("Primeira Consulta", 90),
    FOLLOW_UP("Retorno", 60),
    ACUPUNCTURE("Acupuntura", 60),
    MOXIBUSTION("Moxibustão", 45),
    CUPPING("Ventosaterapia", 30),
    AURICULOTHERAPY("Auriculoterapia", 30),
    EVALUATION("Avaliação", 60)
}

@Serializable
data class Appointment(
    val id: Long = 0L,
    val patientId: Long,
    val patientName: String,
    val date: String,       // ISO: yyyy-MM-dd
    val time: String,       // HH:mm
    val durationMin: Int = 60,
    val type: String = AppointmentType.ACUPUNCTURE.name,
    val status: String = AppointmentStatus.SCHEDULED.name,
    val notes: String = "",
    val sessionNumber: Int = 1,
    val valueBrl: Double = 0.0,
    val paid: Boolean = false,
    val createdAt: String = ""
)
