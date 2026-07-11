package com.bioacupunt.relatorios.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.util.Result
import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.usecase.ObserveReports
import com.bioacupunt.relatorios.domain.usecase.RelatoriosUseCases
import com.bioacupunt.relatorios.domain.usecase.SaveReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RelatoriosUiState(
    val reports: List<Report> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class RelatoriosViewModelFactory(private val cases: RelatoriosUseCases) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RelatoriosViewModel(cases) as T
    }
}

class RelatoriosViewModel(private val cases: RelatoriosUseCases) : ViewModel() {
    private val _state = MutableStateFlow(RelatoriosUiState())
    val state: StateFlow<RelatoriosUiState> = _state.asStateFlow()

    init { observe() }

    fun observe() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            cases.observe()
                .catch { e -> _state.update { it.copy(loading = false, error = it.error ?: e.localizedMessage.orEmpty()) } }
                .collect { list ->
                    _state.update { it.copy(reports = list, loading = false, error = null) }
                }
        }
    }

    fun generate(report: Report) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = cases.save(report)
            if (result is Result.Error) {
                _state.update { it.copy(loading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}
