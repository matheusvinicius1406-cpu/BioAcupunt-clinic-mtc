package com.bioacupunt.prontuario.domain.model

enum class ExamResultTag(val label: String) {
    NORMAL("Normal"),
    ALTERED("Alterado"),
    PENDING("Pendente"),
}

data class VitalSign(
    val id: Long = 0,
    val patientId: Long,
    val label: String,
    val value: String,
    val recordedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class LabExam(
    val id: Long = 0,
    val patientId: Long,
    val name: String,
    val date: String = "",
    val resultTag: ExamResultTag = ExamResultTag.PENDING,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Medication(
    val id: Long = 0,
    val patientId: Long,
    val name: String,
    val info: String = "",
    val active: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Allergy(
    val id: Long = 0,
    val patientId: Long,
    val description: String,
    val createdAt: String = "",
    val updatedAt: String = "",
)
