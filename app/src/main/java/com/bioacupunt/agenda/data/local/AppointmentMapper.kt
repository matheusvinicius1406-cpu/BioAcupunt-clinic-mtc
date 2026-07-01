package com.bioacupunt.agenda.data.local

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType

fun AppointmentEntity.toDomain(): Appointment {
    return Appointment(
        id = id,
        patientId = patientId,
        patientName = patientName,
        date = date,
        time = time,
        durationMin = durationMin,
        type = type,
        status = status,
        notes = notes,
        sessionNumber = 1,
        valueBrl = valueBrl,
        paid = paid,
        createdAt = createdAt
    )
}

fun Appointment.toEntity(now: String = ""): AppointmentEntity {
    val ts = now.ifBlank { java.time.Instant.now().toString() }
    return AppointmentEntity(
        id = id,
        patientId = patientId,
        patientName = patientName,
        date = date,
        time = time,
        durationMin = durationMin,
        type = type,
        status = status,
        notes = notes,
        valueBrl = valueBrl,
        paid = paid,
        createdAt = createdAt,
        updatedAt = ts,
        lastModified = ts
    )
}
