package com.bioacupunt.patient.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.domain.usecase.CreatePatient
import com.bioacupunt.patient.domain.usecase.GetPatients
import com.bioacupunt.sync.SyncScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

class PatientsViewModelFactory(
    private val getPatients: GetPatients,
    private val createPatient: CreatePatient,
    private val syncScheduler: SyncScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PatientsViewModel(getPatients, createPatient, syncScheduler) as T
    }
}

class PatientsViewModel(
    private val getPatients: GetPatients,
    private val createPatient: CreatePatient,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(PatientsUiState())
    val state: StateFlow<PatientsUiState> = _state.asStateFlow()

    init {
        loadPatients()
    }

    private fun loadPatients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getPatients()
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
                }
                .collect { patients ->
                    _state.update { it.copy(isLoading = false, patients = patients, error = null) }
                }
        }
    }

    fun onEvent(event: PatientsEvent) {
        when (event) {
            PatientsEvent.OnLoad -> loadPatients()
            is PatientsEvent.CreatePatient -> createNewPatient(event.name)
            PatientsEvent.OnCreateClick -> Unit
            is PatientsEvent.OnErrorShown -> _state.update { it.copy(error = null) }
        }
    }

    private fun createNewPatient(name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Nome não pode ser vazio.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val now = Instant.now().toString()
                val patient = Patient(
                    id = 0L,
                    tenantId = 1L,
                    name = name.trim(),
                    document = null,
                    createdAt = now,
                    updatedAt = now,
                    status = "ACTIVE"
                )
                createPatient(patient)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
