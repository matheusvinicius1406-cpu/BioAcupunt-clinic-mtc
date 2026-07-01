package com.bioacupunt.prontuario.data.local

import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry

fun ProntuarioEntity.toDomain() = Prontuario(
    patientId = patientId,
    summary = summary,
    mainComplaint = mainComplaint,
    diagnosis = diagnosis,
    treatmentPlan = treatmentPlan
)

fun ProntuarioEntryEntity.toDomain() = ProntuarioEntry(
    id = id,
    patientId = patientId,
    doctorName = doctorName,
    date = date,
    type = when (type) {
        com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.ANAMNESE.name -> com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.ANAMNESE
        com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.EXAM.name -> com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.EXAM
        com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.OBSERVATION.name -> com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.OBSERVATION
        else -> com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.EVOLUTION
    },
    body = body,
    attachmentsJson = attachmentsJson,
    syncedAt = syncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
