package com.bioacupunt.patient.presentation

import com.bioacupunt.patient.domain.model.Patient

data class ProntuarioUiState(
    val patients: List<Patient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
