package com.bioacupunt.pharma.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.pharma.domain.model.ClasseTerapeuticaSummary
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.repository.FormularioMedicamentoRepository
import com.bioacupunt.pharma.domain.repository.MedicamentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FarmacologiaUiState(
    val query: String = "",
    val results: List<Medicamento> = emptyList(),
    val isSearching: Boolean = false,
    val selected: Medicamento? = null,
    /** Null = sem curadoria; presente mas RASCUNHO = ainda não vale pra consulta clínica. */
    val selectedFormulario: FormularioMedicamento? = null,
    val loadingDetail: Boolean = false,
    // ── Navegação por classe terapêutica (sem precisar buscar) ──
    val classes: List<ClasseTerapeuticaSummary> = emptyList(),
    val loadingClasses: Boolean = false,
    val selectedClasse: String? = null,
    val classResults: List<Medicamento> = emptyList(),
) {
    /** A tela só mostra a seção clínica quando isto é true — nunca com um rascunho. */
    val hasApprovedClinicalContent: Boolean
        get() = selectedFormulario?.status == FormularioStatus.APROVADO
}

/**
 * BioAcupunt Pharma Library — biblioteca de consulta, funciona sem paciente
 * selecionado. Identificação vem sempre do catálogo ANVISA; a seção clínica só
 * aparece quando existe [FormularioMedicamento] aprovado (ver [FarmacologiaUiState.hasApprovedClinicalContent]) —
 * nunca mostra a seção vazia em silêncio nem finge que ausência de curadoria é ausência de risco.
 */
class FarmacologiaViewModel(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val tenantManager: TenantManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FarmacologiaUiState())
    val state: StateFlow<FarmacologiaUiState> = _state.asStateFlow()

    init {
        loadClasses()
    }

    fun loadClasses() {
        viewModelScope.launch {
            _state.update { it.copy(loadingClasses = true) }
            val classes = runCatching { medicamentoRepository.listClasses() }.getOrDefault(emptyList())
            _state.update { it.copy(classes = classes, loadingClasses = false) }
        }
    }

    fun selectClasse(classe: String) {
        _state.update { it.copy(selectedClasse = classe, classResults = emptyList()) }
        viewModelScope.launch {
            val results = runCatching { medicamentoRepository.getByClasse(classe) }.getOrDefault(emptyList())
            _state.update { it.copy(classResults = results) }
        }
    }

    fun clearClasseSelection() = _state.update { it.copy(selectedClasse = null, classResults = emptyList()) }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = medicamentoRepository.search(query)
            _state.update { it.copy(results = results, isSearching = false) }
        }
    }

    fun selectMedicamento(medicamento: Medicamento) {
        _state.update { it.copy(selected = medicamento, selectedFormulario = null, loadingDetail = true) }
        viewModelScope.launch {
            val tenantId = tenantManager.requireTenantId()
            val formulario = runCatching {
                formularioMedicamentoRepository.getById(medicamento.id, tenantId)
            }.getOrNull()
            _state.update { it.copy(selectedFormulario = formulario, loadingDetail = false) }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = null, selectedFormulario = null) }
    }
}

class FarmacologiaViewModelFactory(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val tenantManager: TenantManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FarmacologiaViewModel(medicamentoRepository, formularioMedicamentoRepository, tenantManager) as T
}
