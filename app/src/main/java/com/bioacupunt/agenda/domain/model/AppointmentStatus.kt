package com.bioacupunt.agenda.domain.model

enum class AppointmentStatus(val name: String, val label: String, val color: Long) {
    SCHEDULED("SCHEDULED", "Agendado", 0xFF90CAF9),
    CONFIRMED("CONFIRMED", "Confirmado", 0xFF81C784),
    CANCELLED("CANCELLED", "Cancelado", 0xFFEF5350),
    COMPLETED("COMPLETED", "Finalizado", 0xFF66BB6A),
    NO_SHOW("NO_SHOW", "Falta", 0xFFFF8A65),
    IN_PROGRESS("IN_PROGRESS", "Em atendimento", 0xFFFFB74D)
}
