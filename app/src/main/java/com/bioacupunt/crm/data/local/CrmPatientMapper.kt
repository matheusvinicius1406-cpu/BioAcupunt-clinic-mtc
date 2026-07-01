package com.bioacupunt.crm.data.local

import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage

fun CrmPatientEntity.toDomain(): CrmPatient {
    return CrmPatient(
        id = id,
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

fun CrmPatient.toEntity(now: String = ""): CrmPatientEntity {
    val ts = now.ifBlank { java.time.Instant.now().toString() }
    return CrmPatientEntity(
        id = id,
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
        lastModified = ts
    )
}
