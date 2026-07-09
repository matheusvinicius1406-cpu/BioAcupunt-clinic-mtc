package com.bioacupunt.prontuario.presentation

import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry

data class ProntuarioUiState(
    val patientId: Long = 0L,
    val prontuario: Prontuario? = null,
    val entries: List<ProntuarioEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
) {
    val summary: String get() = prontuario?.summary.orEmpty()
    val mainComplaint: String get() = prontuario?.mainComplaint.orEmpty()
    val diagnosis: String get() = prontuario?.diagnosis.orEmpty()
    val treatmentPlan: String get() = prontuario?.treatmentPlan.orEmpty()
}
