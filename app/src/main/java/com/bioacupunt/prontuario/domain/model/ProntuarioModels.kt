package com.bioacupunt.prontuario.domain.model

enum class ProntuarioEntryType(val label: String, val color: Long) {
    ANAMNESE("Anamnese", 0xFF90CAF9),
    EVOLUTION("Evolução", 0xFF81C784),
    EXAM("Exame", 0xFFFFF176),
    OBSERVATION("Observação", 0xFFFFCC80)
}

data class ProntuarioEntry(
    val id: Long = 0,
    val patientId: Long,
    val doctorName: String = "",
    val date: String = "",
    val type: ProntuarioEntryType = ProntuarioEntryType.EVOLUTION,
    val body: String = "",
    val attachmentsJson: String = "[]",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class Prontuario(
    val patientId: Long,
    val summary: String = "",
    val mainComplaint: String = "",
    val diagnosis: String = "",
    val treatmentPlan: String = ""
)
