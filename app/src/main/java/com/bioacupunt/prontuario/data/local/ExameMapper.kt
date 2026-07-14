package com.bioacupunt.prontuario.data.local

import com.bioacupunt.prontuario.domain.model.Allergy
import com.bioacupunt.prontuario.domain.model.ExamResultTag
import com.bioacupunt.prontuario.domain.model.LabExam
import com.bioacupunt.prontuario.domain.model.Medication
import com.bioacupunt.prontuario.domain.model.VitalSign

fun VitalSignEntity.toDomain() = VitalSign(
    id = id, patientId = patientId, label = label, value = value,
    recordedAt = recordedAt, createdAt = createdAt, updatedAt = updatedAt,
)

fun LabExamEntity.toDomain() = LabExam(
    id = id, patientId = patientId, name = name, date = date,
    resultTag = runCatching { ExamResultTag.valueOf(resultTag) }.getOrDefault(ExamResultTag.PENDING),
    notes = notes, createdAt = createdAt, updatedAt = updatedAt,
)

fun MedicationEntity.toDomain() = Medication(
    id = id, patientId = patientId, name = name, info = info,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun AllergyEntity.toDomain() = Allergy(
    id = id, patientId = patientId, description = description,
    createdAt = createdAt, updatedAt = updatedAt,
)
