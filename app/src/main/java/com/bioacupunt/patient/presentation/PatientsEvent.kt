package com.bioacupunt.patient.presentation

sealed interface PatientsEvent {
    data object OnLoad : PatientsEvent
    data class CreatePatient(val name: String) : PatientsEvent
    data object OnCreateClick : PatientsEvent
    data class OnErrorShown(val message: String) : PatientsEvent
}
