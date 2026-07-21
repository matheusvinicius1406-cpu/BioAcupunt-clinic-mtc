package com.bioacupunt.agenda.data.local

import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.model.AppointmentType

fun AppointmentEntity.toDomain(): Appointment {
    return Appointment(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        patientName = patientName,
        date = date,
        time = time,
        durationMin = durationMin,
        type = type,
        status = status,
        notes = notes,
        sessionNumber = sessionNumber,
        reminderMinutesBefore = reminderMinutesBefore,
        valueBrl = valueBrl,
        paid = paid,
        createdAt = createdAt
    )
}

/** See CrmPatientMapper.toEntity for what [identity] and [pendingSync] mean. */
fun Appointment.toEntity(
    now: String = "",
    identity: com.bioacupunt.sync.SyncIdentity = com.bioacupunt.sync.SyncIdentity.new(),
    pendingSync: Boolean = true,
): AppointmentEntity {
    val ts = now.ifBlank { java.time.Instant.now().toString() }
    return AppointmentEntity(
        id = id,
        tenantId = tenantId,
        patientId = patientId,
        patientName = patientName,
        date = date,
        time = time,
        durationMin = durationMin,
        type = type,
        status = status,
        notes = notes,
        sessionNumber = sessionNumber,
        reminderMinutesBefore = reminderMinutesBefore,
        valueBrl = valueBrl,
        paid = paid,
        createdAt = createdAt,
        updatedAt = ts,
        lastModified = ts,
        pendingSync = pendingSync,
        clientId = identity.clientId,
        serverId = identity.serverId,
        baseRev = identity.baseRev
    )
}
