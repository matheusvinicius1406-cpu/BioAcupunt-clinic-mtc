package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProntuarioUiState(
    val patientId: Long = 0L,
    val loading: Boolean = false,
    val summary: String = "",
    val mainComplaint: String = "",
    val diagnosis: String = "",
    val treatmentPlan: String = "",
    val entries: List<ProntuarioEntry> = emptyList(),
    val error: String? = null
)

class ProntuarioViewModelFactory(
    private val cases: ProntuarioUseCases
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProntuarioViewModel(cases) as T
    }
}

class ProntuarioViewModel(private val cases: ProntuarioUseCases) : ViewModel() {
    private val _state = MutableStateFlow(ProntuarioUiState())
    val state: StateFlow<ProntuarioUiState> = _state.asStateFlow()

    fun load(patientId: Long) {
        _state.update { it.copy(patientId = patientId, error = null) }
        viewModelScope.launch {
            cases.observeEntries(patientId).collect { list ->
                _state.update { it.copy(entries = list) }
            }
        }
        viewModelScope.launch {
            cases.getProntuario(patientId).collect { pront ->
                if (pront != null) {
                    _state.update {
                        it.copy(
                            summary = pront.summary,
                            mainComplaint = pront.mainComplaint,
                            diagnosis = pront.diagnosis,
                            treatmentPlan = pront.treatmentPlan
                        )
                    }
                }
            }
        }
    }

    fun updateHeader(summary: String? = null, mainComplaint: String? = null, diagnosis: String? = null, treatmentPlan: String? = null) {
        _state.update { current ->
            current.copy(
                summary = summary ?: current.summary,
                mainComplaint = mainComplaint ?: current.mainComplaint,
                diagnosis = diagnosis ?: current.diagnosis,
                treatmentPlan = treatmentPlan ?: current.treatmentPlan
            )
        }
        val pid = _state.value.patientId
        if (pid <= 0L) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val pront = Prontuario(
                patientId = pid,
                summary = _state.value.summary,
                mainComplaint = _state.value.mainComplaint,
                diagnosis = _state.value.diagnosis,
                treatmentPlan = _state.value.treatmentPlan
            )
            val result = cases.saveProntuario(pront)
            if (result is Result.Error) {
                _state.update { it.copy(loading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun addEntry(body: String, type: com.bioacupunt.prontuario.domain.model.ProntuarioEntryType) {
        val pid = _state.value.patientId
        if (pid <= 0L || body.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val entry = ProntuarioEntry(
                patientId = pid,
                doctorName = "",
                date = java.time.LocalDate.now().toString(),
                type = type,
                body = body.trim()
            )
            val result = cases.addEntry(entry)
            if (result is Result.Error) {
                _state.update { it.copy(loading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
