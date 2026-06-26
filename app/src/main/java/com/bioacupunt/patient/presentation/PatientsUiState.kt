package com.bioacupunt.patient.presentation

data class PatientsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val patients: List<com.bioacupunt.patient.domain.model.Patient> = emptyList()
)
