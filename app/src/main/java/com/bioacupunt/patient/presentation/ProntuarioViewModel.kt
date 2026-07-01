package com.bioacupunt.patient.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.patient.domain.usecase.GetPatients
import com.bioacupunt.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProntuarioViewModelFactory(
    private val getPatients: GetPatients,
    private val scheduler: SyncScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProntuarioViewModel(getPatients, scheduler) as T
    }
}

class ProntuarioViewModel(
    private val getPatients: GetPatients,
    private val scheduler: SyncScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(ProntuarioUiState())
    val state: StateFlow<ProntuarioUiState> = _state.asStateFlow()

    init {
        loadPatients()
    }

    fun loadPatients() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getPatients()
                .catch { _state.value = _state.value.copy(isLoading = false, error = it.localizedMessage) }
                .collect { patients ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        patients = patients,
                        error = null
                    )
                }
        }
    }
}
