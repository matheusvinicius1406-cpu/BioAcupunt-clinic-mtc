package com.bioacupunt.pharma.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.domain.model.ClasseTerapeuticaSummary
import com.bioacupunt.pharma.domain.model.EfeitoAdverso
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.InteracaoMedicamentosa
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.SeveridadeInteracao
import com.bioacupunt.pharma.domain.repository.FormularioMedicamentoRepository
import com.bioacupunt.pharma.domain.repository.MedicamentoRepository
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FarmacologiaCuradoriaUiState(
    val query: String = "",
    val results: List<Medicamento> = emptyList(),
    val isSearching: Boolean = false,
    val selected: Medicamento? = null,
    val draft: FormularioMedicamento? = null,
    // Busca auxiliar pra escolher "outro medicamento" numa interação.
    val interacaoQuery: String = "",
    val interacaoResults: List<Medicamento> = emptyList(),
    val interacaoOutro: Medicamento? = null,
    val interacaoSeveridade: SeveridadeInteracao = SeveridadeInteracao.MODERADA,
    val interacaoDescricao: String = "",
    val efeitoDescricao: String = "",
    val efeitoFrequencia: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null,
    // ── Navegação por classe terapêutica (sem precisar buscar) ──
    val classes: List<ClasseTerapeuticaSummary> = emptyList(),
    val loadingClasses: Boolean = false,
    val selectedClasse: String? = null,
    val classResults: List<Medicamento> = emptyList(),
) {
    val canApprove: Boolean get() = draft?.meetsApprovalMinimum == true
}

/**
 * Curadoria Farmacológica — onde a médica preenche, com a bula em mãos, a camada
 * clínica de um item do catálogo (posologia/interação/contraindicação/MTC). Nunca gera
 * nada: todo campo começa vazio e só existe se ela digitar. `approve()` recusa quando
 * [FormularioMedicamento.meetsApprovalMinimum] é falso — mesmo espírito do gate de
 * `citation` vazia em `LibraryIngestion.stage` (R4).
 */
class FarmacologiaCuradoriaViewModel(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val tenantManager: TenantManager,
    private val currentUser: () -> String,
) : ViewModel() {

    private val _state = MutableStateFlow(FarmacologiaCuradoriaUiState())
    val state: StateFlow<FarmacologiaCuradoriaUiState> = _state.asStateFlow()

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
        _state.update { it.copy(selected = medicamento, draft = null) }
        viewModelScope.launch {
            val tenantId = tenantManager.requireTenantId()
            val existing = runCatching { formularioMedicamentoRepository.getById(medicamento.id, tenantId) }.getOrNull()
            _state.update {
                it.copy(draft = existing ?: FormularioMedicamento(medicamentoId = medicamento.id, tenantId = tenantId))
            }
        }
    }

    fun clearSelection() = _state.update { it.copy(selected = null, draft = null) }

    private fun updateDraft(transform: (FormularioMedicamento) -> FormularioMedicamento) {
        _state.update { s -> s.draft?.let { s.copy(draft = transform(it)) } ?: s }
    }

    fun onPosologiaAdultoChanged(v: String) = updateDraft { it.copy(posologiaAdulto = v) }
    fun onPosologiaPediatricaChanged(v: String) = updateDraft { it.copy(posologiaPediatrica = v) }
    fun onPosologiaIdosoChanged(v: String) = updateDraft { it.copy(posologiaIdoso = v) }
    fun onPosologiaRenalChanged(v: String) = updateDraft { it.copy(posologiaRenal = v) }
    fun onPosologiaHepaticaChanged(v: String) = updateDraft { it.copy(posologiaHepatica = v) }
    fun onViaChanged(v: String) = updateDraft { it.copy(viaAdministracao = v) }
    fun onVisaoMtcChanged(v: String) = updateDraft { it.copy(visaoIntegrativaMtc = v) }
    fun onAlergenosChanged(csv: String) = updateDraft {
        it.copy(alergenos = csv.split(',').map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() })
    }

    fun toggleContraindicacaoAbsoluta(flag: ClinicalFlag) = updateDraft {
        val set = it.contraindicacoesAbsolutas.toMutableSet()
        if (!set.add(flag)) set.remove(flag)
        it.copy(contraindicacoesAbsolutas = set.toList(), contraindicacoesRelativas = it.contraindicacoesRelativas - flag)
    }

    fun toggleContraindicacaoRelativa(flag: ClinicalFlag) = updateDraft {
        val set = it.contraindicacoesRelativas.toMutableSet()
        if (!set.add(flag)) set.remove(flag)
        it.copy(contraindicacoesRelativas = set.toList(), contraindicacoesAbsolutas = it.contraindicacoesAbsolutas - flag)
    }

    // -- Interações (mini-formulário de adição) -----------------------------------

    fun onInteracaoQueryChanged(query: String) {
        _state.update { it.copy(interacaoQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(interacaoResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = medicamentoRepository.search(query)
            _state.update { it.copy(interacaoResults = results) }
        }
    }

    fun onInteracaoOutroSelected(medicamento: Medicamento) =
        _state.update { it.copy(interacaoOutro = medicamento, interacaoResults = emptyList(), interacaoQuery = medicamento.nomeComercial) }

    fun onInteracaoSeveridadeChanged(sev: SeveridadeInteracao) = _state.update { it.copy(interacaoSeveridade = sev) }
    fun onInteracaoDescricaoChanged(v: String) = _state.update { it.copy(interacaoDescricao = v) }

    fun addInteracao() {
        val s = _state.value
        val outro = s.interacaoOutro ?: return
        if (s.interacaoDescricao.isBlank()) return
        updateDraft {
            it.copy(
                interacoes = it.interacoes + InteracaoMedicamentosa(
                    outroMedicamentoId = outro.id,
                    outroNome = outro.nomeComercial,
                    severidade = s.interacaoSeveridade,
                    descricao = s.interacaoDescricao.trim(),
                ),
            )
        }
        _state.update { it.copy(interacaoOutro = null, interacaoQuery = "", interacaoResults = emptyList(), interacaoDescricao = "") }
    }

    fun removeInteracao(index: Int) = updateDraft { it.copy(interacoes = it.interacoes.filterIndexed { i, _ -> i != index }) }

    // -- Efeitos adversos -----------------------------------------------------------

    fun onEfeitoDescricaoChanged(v: String) = _state.update { it.copy(efeitoDescricao = v) }
    fun onEfeitoFrequenciaChanged(v: String) = _state.update { it.copy(efeitoFrequencia = v) }

    fun addEfeitoAdverso() {
        val s = _state.value
        if (s.efeitoDescricao.isBlank()) return
        updateDraft {
            it.copy(efeitosAdversos = it.efeitosAdversos + EfeitoAdverso(s.efeitoDescricao.trim(), s.efeitoFrequencia.trim()))
        }
        _state.update { it.copy(efeitoDescricao = "", efeitoFrequencia = "") }
    }

    fun removeEfeitoAdverso(index: Int) = updateDraft { it.copy(efeitosAdversos = it.efeitosAdversos.filterIndexed { i, _ -> i != index }) }

    // -- Persistência -----------------------------------------------------------------

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            when (val result = formularioMedicamentoRepository.save(draft.copy(autor = currentUser()))) {
                is Result.Error -> _state.update { it.copy(saving = false, error = result.kind.userMessage) }
                is Result.Success -> _state.update {
                    it.copy(saving = false, draft = result.data, savedMessage = "Rascunho salvo.")
                }
                Result.Loading -> Unit
            }
        }
    }

    fun approve() {
        val draft = _state.value.draft ?: return
        val medicamentoId = draft.medicamentoId
        val tenantId = draft.tenantId
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            // Garante que o que está na tela foi persistido antes de tentar aprovar.
            formularioMedicamentoRepository.save(draft.copy(autor = currentUser()))
            when (val result = formularioMedicamentoRepository.approve(medicamentoId, tenantId, currentUser())) {
                is Result.Error -> _state.update { it.copy(saving = false, error = result.kind.userMessage) }
                is Result.Success -> _state.update {
                    it.copy(saving = false, draft = result.data, savedMessage = "Formulário aprovado — disponível pra Smart Prescription.")
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, savedMessage = null) }
}

class FarmacologiaCuradoriaViewModelFactory(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val tenantManager: TenantManager,
    private val currentUser: () -> String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FarmacologiaCuradoriaViewModel(
        medicamentoRepository, formularioMedicamentoRepository, tenantManager, currentUser,
    ) as T
}
