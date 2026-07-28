package com.bioacupunt.educacao.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.educacao.domain.model.SimulatedCase
import com.bioacupunt.educacao.domain.repository.SimulatedCaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimuladorUiState(
    /** Caso fixo + casos aprovados na Curadoria — cresce junto com o acervo da Biblioteca. */
    val cases: List<SimulatedCase> = emptyList(),
    val selectedCase: SimulatedCase? = null,
    val showAnswer: Boolean = false,
)

/** Backs the "Casos Clínicos" mode of the Simulador — antes um único caso fixo hardcoded. */
class SimuladorViewModel(
    private val repository: SimulatedCaseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SimuladorUiState())
    val state: StateFlow<SimuladorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll()
                .catch { emit(emptyList()) }
                .collect { cases -> _state.update { it.copy(cases = cases) } }
        }
    }

    fun selectCase(case: SimulatedCase) = _state.update { it.copy(selectedCase = case, showAnswer = false) }
    fun clearSelection() = _state.update { it.copy(selectedCase = null, showAnswer = false) }
    fun toggleAnswer() = _state.update { it.copy(showAnswer = !it.showAnswer) }
}

class SimuladorViewModelFactory(
    private val repository: SimulatedCaseRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SimuladorViewModel(repository) as T
    }
}
