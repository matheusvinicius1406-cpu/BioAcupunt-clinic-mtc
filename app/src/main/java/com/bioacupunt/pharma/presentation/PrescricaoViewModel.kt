package com.bioacupunt.pharma.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.Prescricao
import com.bioacupunt.pharma.domain.repository.FormularioMedicamentoRepository
import com.bioacupunt.pharma.domain.repository.MedicamentoRepository
import com.bioacupunt.pharma.domain.repository.PrescricaoRepository
import com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine
import com.bioacupunt.pharma.domain.safety.PharmaVerdict
import com.bioacupunt.prontuario.domain.repository.ExameRepository
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class PrescricaoUiState(
    val query: String = "",
    val results: List<Medicamento> = emptyList(),
    val isSearching: Boolean = false,
    val selected: Medicamento? = null,
    val verdict: PharmaVerdict? = null,
    val checkingVerdict: Boolean = false,
    val activePrescricoes: List<Prescricao> = emptyList(),
    val dose: String = "",
    val frequencia: String = "",
    val duracao: String = "",
    val via: String = "",
    val observacoes: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null,
)

/**
 * Smart Prescription — busca no catálogo, roda [PharmaSafetyEngine] contra o perfil de
 * risco PERMANENTE da paciente ([MtcAssessmentRepository.standingFlags], não só a
 * consulta atual — mesmo raciocínio de "esquecer é o que o software existe pra pegar"
 * já aplicado ao motor clínico), alergias e medicações ativas, e só então permite salvar.
 *
 * Um FORBIDDEN nunca é contornável em silêncio: [confirmPrescricao] recusa quando
 * [PharmaVerdict.isBlocked]; só [overridePrescricaoVeto], com justificativa ≥10
 * caracteres, grava por cima.
 */
class PrescricaoViewModel(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val prescricaoRepository: PrescricaoRepository,
    private val exameRepository: ExameRepository,
    private val mtcAssessmentRepository: MtcAssessmentRepository,
    private val safetyEngine: PharmaSafetyEngine,
    private val tenantManager: TenantManager,
    private val patientId: Long,
    private val currentUser: () -> String,
) : ViewModel() {

    private val _state = MutableStateFlow(PrescricaoUiState())
    val state: StateFlow<PrescricaoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prescricaoRepository.observeActiveByPatient(patientId).collect { list ->
                _state.update { it.copy(activePrescricoes = list) }
            }
        }
    }

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
        _state.update {
            it.copy(
                selected = medicamento, verdict = null, checkingVerdict = true,
                dose = "", frequencia = "", duracao = "", via = "", observacoes = "",
            )
        }
        viewModelScope.launch {
            val tenantId = tenantManager.requireTenantId()
            val formulario = runCatching { formularioMedicamentoRepository.getById(medicamento.id, tenantId) }.getOrNull()
            val flags = runCatching { mtcAssessmentRepository.standingFlags(patientId) }.getOrDefault(emptySet())
            val allergies = runCatching { exameRepository.observeAllergies(patientId).first() }
                .getOrDefault(emptyList())
                .map { it.description }
            val activeIds = _state.value.activePrescricoes.mapNotNull { it.medicamentoId }
            val activeFormularios = runCatching {
                formularioMedicamentoRepository.getApprovedByIds(activeIds, tenantId)
            }.getOrDefault(emptyList())

            val verdict = safetyEngine.evaluate(medicamento, formulario, flags, allergies, activeFormularios)
            _state.update {
                it.copy(
                    verdict = verdict,
                    checkingVerdict = false,
                    dose = formulario?.posologiaAdulto.orEmpty(),
                    via = formulario?.viaAdministracao.orEmpty(),
                )
            }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = null, verdict = null) }
    }

    fun onDoseChanged(value: String) = _state.update { it.copy(dose = value) }
    fun onFrequenciaChanged(value: String) = _state.update { it.copy(frequencia = value) }
    fun onDuracaoChanged(value: String) = _state.update { it.copy(duracao = value) }
    fun onViaChanged(value: String) = _state.update { it.copy(via = value) }
    fun onObservacoesChanged(value: String) = _state.update { it.copy(observacoes = value) }

    /** Recusa em silêncio quando bloqueado — a tela deve desabilitar o botão nesse caso; ver [overridePrescricaoVeto]. */
    fun confirmPrescricao() {
        val verdict = _state.value.verdict
        if (verdict != null && verdict.isBlocked) return
        persist(overrideReason = "", overrideBy = "")
    }

    fun overridePrescricaoVeto(reason: String) {
        if (reason.trim().length < 10) return
        persist(overrideReason = reason.trim(), overrideBy = currentUser())
    }

    private fun persist(overrideReason: String, overrideBy: String) {
        val s = _state.value
        val medicamento = s.selected ?: return
        if (s.dose.isBlank() || s.frequencia.isBlank()) {
            _state.update { it.copy(error = "Informe ao menos dose e frequência.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val prescricao = Prescricao(
                patientId = patientId,
                tenantId = tenantManager.requireTenantId(),
                medicamentoId = medicamento.id,
                dose = s.dose.trim(),
                frequencia = s.frequencia.trim(),
                duracao = s.duracao.trim(),
                viaAdministracao = s.via.trim(),
                observacoes = s.observacoes.trim(),
                prescritoPor = currentUser(),
                overrideReason = overrideReason,
                overrideBy = overrideBy,
                overrideAt = if (overrideReason.isNotBlank()) Instant.now().toString() else "",
            )
            when (val result = prescricaoRepository.save(prescricao)) {
                is Result.Error -> _state.update { it.copy(saving = false, error = result.kind.userMessage) }
                is Result.Success -> _state.update {
                    it.copy(
                        saving = false, selected = null, verdict = null,
                        dose = "", frequencia = "", duracao = "", via = "", observacoes = "",
                        savedMessage = "Prescrição registrada.",
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    fun deactivate(id: Long) {
        viewModelScope.launch {
            val result = prescricaoRepository.deactivate(id)
            if (result is Result.Error) _state.update { it.copy(error = result.kind.userMessage) }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, savedMessage = null) }
}

class PrescricaoViewModelFactory(
    private val medicamentoRepository: MedicamentoRepository,
    private val formularioMedicamentoRepository: FormularioMedicamentoRepository,
    private val prescricaoRepository: PrescricaoRepository,
    private val exameRepository: ExameRepository,
    private val mtcAssessmentRepository: MtcAssessmentRepository,
    private val safetyEngine: PharmaSafetyEngine,
    private val tenantManager: TenantManager,
    private val patientId: Long,
    private val currentUser: () -> String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PrescricaoViewModel(
        medicamentoRepository, formularioMedicamentoRepository, prescricaoRepository,
        exameRepository, mtcAssessmentRepository, safetyEngine, tenantManager, patientId, currentUser,
    ) as T
}
