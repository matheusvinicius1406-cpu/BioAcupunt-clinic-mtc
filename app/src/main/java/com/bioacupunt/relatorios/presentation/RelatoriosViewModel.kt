package com.bioacupunt.relatorios.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.util.Result
import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.usecase.GenerateReportUseCase
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
    val generating: Boolean = false,
    val error: String? = null,
    val generateError: String? = null,
    val lastGenerated: Report? = null,
)

class RelatoriosViewModelFactory(
    private val cases: RelatoriosUseCases,
    private val generateReport: GenerateReportUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RelatoriosViewModel(cases, generateReport) as T
    }
}

class RelatoriosViewModel(
    private val cases: RelatoriosUseCases,
    private val generateReport: GenerateReportUseCase,
) : ViewModel() {
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

    /**
     * Gera o relatório a partir do prontuário REAL do paciente (nome digitado no
     * diálogo resolve contra o CRM) e salva. Erro (paciente não encontrado, nome
     * ambíguo, falha de banco) chega em [RelatoriosUiState.generateError] — nunca
     * é descartado em silêncio, nunca gera relatório vazio.
     */
    fun generate(type: String, title: String, patientName: String) {
        if (_state.value.generating) return
        viewModelScope.launch {
            _state.update { it.copy(generating = true, generateError = null) }
            when (val result = generateReport(type, title, patientName)) {
                is Result.Success -> {
                    val saved = cases.save(result.data)
                    _state.update {
                        if (saved is Result.Error) {
                            it.copy(generating = false, generateError = saved.kind.userMessage)
                        } else {
                            it.copy(generating = false, lastGenerated = savedOr(result.data, saved))
                        }
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(generating = false, generateError = result.kind.userMessage)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun savedOr(generated: Report, saved: Result<Report>): Report =
        (saved as? Result.Success)?.data ?: generated

    fun clearGenerateError() = _state.update { it.copy(generateError = null) }

    fun clearLastGenerated() = _state.update { it.copy(lastGenerated = null) }
}
