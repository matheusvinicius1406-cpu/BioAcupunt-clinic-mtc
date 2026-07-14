package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.model.TongueBodyColor
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import com.bioacupunt.prontuario.domain.usecase.ObserveEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvolucaoUiState(
    val patientId: Long = 0,
    /** Oldest -> newest, unlike the repository's newest-first order. */
    val history: List<MtcAssessment> = emptyList(),
    val entries: List<ProntuarioEntry> = emptyList(),
)

/** Deterministic session-over-session comparison — arithmetic and string
 * formatting over real [MtcAssessment] history, never a model call. */
data class EvolucaoComparison(
    val evaFrom: Int?,
    val evaTo: Int?,
    val tongueFrom: TongueBodyColor?,
    val tongueTo: TongueBodyColor?,
) {
    val hasData: Boolean get() = evaFrom != null || tongueFrom != null

    val evaImproved: Boolean get() = evaFrom != null && evaTo != null && evaTo < evaFrom
}

class EvolucaoViewModel(
    private val mtcAssessmentRepository: MtcAssessmentRepository,
    private val observeEntries: ObserveEntries,
    patientId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EvolucaoUiState(patientId = patientId))
    val state: StateFlow<EvolucaoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                mtcAssessmentRepository.observeHistory(patientId),
                observeEntries(patientId),
            ) { history, entries -> history.sortedBy { it.date } to entries }
                .catch { emit(emptyList<MtcAssessment>() to emptyList()) }
                .collect { (history, entries) ->
                    _state.update { it.copy(history = history, entries = entries) }
                }
        }
    }

    /** Average EVA (body-mark intensity) recorded in an assessment, or null if none. */
    fun evaFor(assessment: MtcAssessment): Int? {
        val marks = assessment.bodyMarks
        if (marks.isEmpty()) return null
        return marks.map { it.intensity }.average().toInt()
    }

    fun comparison(): EvolucaoComparison {
        val history = _state.value.history
        val first = history.firstOrNull()
        val last = history.lastOrNull()
        return EvolucaoComparison(
            evaFrom = first?.let { evaFor(it) },
            evaTo = last?.let { evaFor(it) },
            tongueFrom = first?.tongue?.bodyColor?.takeIf { it != TongueBodyColor.UNSET },
            tongueTo = last?.tongue?.bodyColor?.takeIf { it != TongueBodyColor.UNSET },
        )
    }
}

class EvolucaoViewModelFactory(
    private val mtcAssessmentRepository: MtcAssessmentRepository,
    private val observeEntries: ObserveEntries,
    private val patientId: Long,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EvolucaoViewModel(mtcAssessmentRepository, observeEntries, patientId) as T
    }
}
