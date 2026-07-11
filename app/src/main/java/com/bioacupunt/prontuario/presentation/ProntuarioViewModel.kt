package com.bioacupunt.prontuario.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.model.ProntuarioEntryType
import com.bioacupunt.prontuario.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class ProntuarioViewModel(
    private val getProntuario: GetProntuario,
    private val saveProntuario: SaveProntuario,
    private val observeEntries: ObserveEntries,
    private val addEntry: AddEntry,
    private val updateEntryUC: UpdateEntry,
    private val deleteEntry: DeleteEntry
) : ViewModel() {

    private val _patientId = MutableStateFlow<Long>(0L)
    private val _prontuario = MutableStateFlow<Prontuario?>(null)
    private val _entries = mutableStateListOf<ProntuarioEntry>()
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ProntuarioUiState> = combine(
        _patientId,
        _prontuario,
        kotlinx.coroutines.flow.flowOf(_entries),
        _loading,
        _error
    ) { patientId, prontuario, _, loading, error ->
        ProntuarioUiState(
            patientId = patientId,
            prontuario = prontuario,
            entries = _entries.toList(),
            loading = loading,
            error = error
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), ProntuarioUiState())

    fun load(patientId: Long) {
        _patientId.value = patientId
        _prontuario.value = Prontuario(patientId = patientId)
        _entries.clear()

        observeEntries(patientId).onEach { list ->
            _entries.clear()
            _entries.addAll(list)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            getProntuario(patientId).collect { pront ->
                _prontuario.value = pront ?: Prontuario(patientId = patientId)
            }
        }
    }

    fun updateHeader(summary: String? = null, mainComplaint: String? = null, diagnosis: String? = null, treatmentPlan: String? = null) {
        val current = _prontuario.value ?: return
        _prontuario.value = current.copy(
            summary = summary ?: current.summary,
            mainComplaint = mainComplaint ?: current.mainComplaint,
            diagnosis = diagnosis ?: current.diagnosis,
            treatmentPlan = treatmentPlan ?: current.treatmentPlan
        )
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = saveProntuario(_prontuario.value!!)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _error.value = result.kind.userMessage
            }
            _loading.value = false
        }
    }

    fun addSession(body: String, type: ProntuarioEntryType) {
        val pid = _patientId.value
        if (pid <= 0L || body.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val entry = ProntuarioEntry(
                patientId = pid,
                doctorName = "",
                date = java.time.Instant.now().toString(),
                type = type,
                body = body.trim(),
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            val result = addEntry(entry)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _error.value = result.kind.userMessage
            }
            _loading.value = false
        }
    }

    fun updateEntry(id: Long, type: ProntuarioEntryType, body: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val current = _entries.firstOrNull { it.id == id } ?: run {
                _error.value = "Registro não encontrado"
                _loading.value = false
                return@launch
            }
            val entry = current.copy(
                type = type,
                body = body.trim(),
                updatedAt = java.time.Instant.now().toString()
            )
            val result = updateEntryUC(entry)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _error.value = result.kind.userMessage
            }
            _loading.value = false
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = deleteEntry(id)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _error.value = result.kind.userMessage
            }
            _loading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
