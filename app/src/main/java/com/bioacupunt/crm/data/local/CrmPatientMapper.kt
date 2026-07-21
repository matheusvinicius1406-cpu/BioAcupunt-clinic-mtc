package com.bioacupunt.crm.data.local

import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage

fun CrmPatientEntity.toDomain(): CrmPatient {
    return CrmPatient(
        id = id,
        tenantId = tenantId,
        name = name,
        phone = phone,
        email = email,
        birthDate = birthDate,
        stage = stage,
        totalSessions = totalSessions,
        totalRevenueBrl = totalRevenueBrl,
        lastVisit = lastVisit,
        nextAppointment = nextAppointment,
        tags = tags.split(",").filter { it.isNotBlank() },
        notes = notes,
        referralSource = referralSource,
        npsScore = npsScore,
        healthInsurance = healthInsurance,
        mainComplaint = mainComplaint,
        createdAt = createdAt
    )
}

/**
 * @param identity the row's sync identity. Callers that are *saving a local
 *   edit* must pass the existing row's identity (or a fresh one for a new row)
 *   — see CrmPatientRepositoryImpl.save. Defaulting to a new identity here would
 *   silently mint a second client id on every edit, and the server would treat
 *   each save as a brand-new patient.
 * @param pendingSync true when this write originated locally and still has to
 *   reach the server. Only the sync writers pass false, when storing what the
 *   server just sent.
 */
fun CrmPatient.toEntity(
    now: String = "",
    identity: com.bioacupunt.sync.SyncIdentity = com.bioacupunt.sync.SyncIdentity.new(),
    pendingSync: Boolean = true,
): CrmPatientEntity {
    val ts = now.ifBlank { java.time.Instant.now().toString() }
    return CrmPatientEntity(
        id = id,
        tenantId = tenantId,
        name = name,
        phone = phone,
        email = email,
        birthDate = birthDate,
        stage = stage,
        totalSessions = totalSessions,
        totalRevenueBrl = totalRevenueBrl,
        lastVisit = lastVisit,
        nextAppointment = nextAppointment,
        tags = tags.joinToString(","),
        notes = notes,
        referralSource = referralSource,
        npsScore = npsScore,
        healthInsurance = healthInsurance,
        mainComplaint = mainComplaint,
        createdAt = createdAt,
        updatedAt = ts,
        lastModified = ts,
        pendingSync = pendingSync,
        clientId = identity.clientId,
        serverId = identity.serverId,
        baseRev = identity.baseRev
    )
}
