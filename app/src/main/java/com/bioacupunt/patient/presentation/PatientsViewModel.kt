package com.bioacupunt.patient.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bioacupunt.patient.domain.usecase.CreatePatient
import com.bioacupunt.patient.domain.usecase.GetPatients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val getPatients: GetPatients,
    private val createPatient: CreatePatient
) : ViewModel() {

  private val _state = MutableStateFlow(PatientsUiState())
  val state: StateFlow<PatientsUiState> = _state.asStateFlow()

  private val _effects = MutableSharedFlow<PatientsEffect>()
  val effects: SharedFlow<PatientsEffect> = _effects.asSharedFlow()

  fun onEvent(event: PatientsEvent) {
    when (event) {
      PatientsEvent.OnLoad -> loadPatients()
      is PatientsEvent.CreatePatient -> createPatient(event.name)
      PatientsEvent.OnCreateClick -> Unit
      is PatientsEvent.OnErrorShown -> _state.update { it.copy(error = null) }
    }
  }

  private fun loadPatients() {
    _state.update { it.copy(isLoading = true, error = null) }
    viewModelScope.launch {
      runCatching { getPatients() }
        .onSuccess { _state.update { s -> s.copy(patients = it, isLoading = false) } }
        .onFailure { _state.update { s -> s.copy(error = it.message ?: "Erro", isLoading = false) } }
    }
  }

  private fun createPatient(name: String) {
    if (name.isBlank()) {
      _state.update { it.copy(error = "Nome obrigatório") }
      return
    }
    _state.update { it.copy(isLoading = true, error = null) }
    viewModelScope.launch {
      runCatching {
        val created = createPatient(
          patient = com.bioacupunt.patient.domain.model.Patient(
            tenantId = 1,
            name = name.trim(),
            createdAt = "",
            updatedAt = "",
            status = "active"
          )
        )
        created
      }
        .onSuccess {
          _state.update { s -> s.copy(isLoading = false) }
          _effects.emit(PatientsEffect.PatientCreated(it.id))
        }
        .onFailure { _state.update { s -> s.copy(error = it.message ?: "Erro ao criar", isLoading = false) } }
    }
  }
}
