package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.prontuario.domain.model.Allergy
import com.bioacupunt.prontuario.domain.model.ExamResultTag
import com.bioacupunt.prontuario.domain.model.LabExam
import com.bioacupunt.prontuario.domain.model.Medication
import com.bioacupunt.prontuario.domain.model.ProntuarioDocument
import com.bioacupunt.prontuario.domain.model.VitalSign
import com.bioacupunt.prontuario.domain.usecase.ExameUseCases
import com.bioacupunt.prontuario.domain.usecase.ProntuarioDocumentUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExameUiState(
    val patientId: Long = 0,
    val vitals: List<VitalSign> = emptyList(),
    val exams: List<LabExam> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val documents: List<ProntuarioDocument> = emptyList(),
    val error: String? = null,
)

/** Backs the "Exames" and "Documentos" tabs of the Prontuário screen. */
class ExameViewModel(
    private val exameUseCases: ExameUseCases,
    private val documentUseCases: ProntuarioDocumentUseCases,
    private val patientId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ExameUiState(patientId = patientId))
    val state: StateFlow<ExameUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                exameUseCases.observeVitals(patientId),
                exameUseCases.observeExams(patientId),
                exameUseCases.observeMedications(patientId),
                exameUseCases.observeAllergies(patientId),
                documentUseCases.observeDocuments(patientId),
            ) { vitals, exams, meds, allergies, docs ->
                ExameUiState(patientId, vitals, exams, meds, allergies, docs)
            }.catch { e -> _state.update { it.copy(error = e.localizedMessage) } }
                .collect { merged -> _state.update { merged.copy(error = it.error) } }
        }
    }

    fun addVital(label: String, value: String) {
        if (label.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            val now = java.time.LocalDate.now().toString()
            exameUseCases.saveVital(VitalSign(patientId = patientId, label = label.trim(), value = value.trim(), recordedAt = now))
        }
    }

    fun deleteVital(id: Long) = viewModelScope.launch { exameUseCases.deleteVital(id) }

    fun addExam(name: String, date: String, resultTag: ExamResultTag) {
        if (name.isBlank()) return
        viewModelScope.launch {
            exameUseCases.saveExam(LabExam(patientId = patientId, name = name.trim(), date = date, resultTag = resultTag))
        }
    }

    fun deleteExam(id: Long) = viewModelScope.launch { exameUseCases.deleteExam(id) }

    fun addMedication(name: String, info: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            exameUseCases.saveMedication(Medication(patientId = patientId, name = name.trim(), info = info.trim()))
        }
    }

    fun deleteMedication(id: Long) = viewModelScope.launch { exameUseCases.deleteMedication(id) }

    fun addAllergy(description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            exameUseCases.saveAllergy(Allergy(patientId = patientId, description = description.trim()))
        }
    }

    fun deleteAllergy(id: Long) = viewModelScope.launch { exameUseCases.deleteAllergy(id) }

    fun addDocument(name: String, uri: String, mimeType: String, sizeBytes: Long) {
        if (name.isBlank() || uri.isBlank()) return
        viewModelScope.launch {
            val now = java.time.Instant.now().toString()
            documentUseCases.saveDocument(
                ProntuarioDocument(patientId = patientId, name = name, uri = uri, mimeType = mimeType, sizeBytes = sizeBytes, addedAt = now)
            )
        }
    }

    fun deleteDocument(id: Long) = viewModelScope.launch { documentUseCases.deleteDocument(id) }

    fun clearError() = _state.update { it.copy(error = null) }
}

class ExameViewModelFactory(
    private val exameUseCases: ExameUseCases,
    private val documentUseCases: ProntuarioDocumentUseCases,
    private val patientId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ExameViewModel(exameUseCases, documentUseCases, patientId) as T
}
