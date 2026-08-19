package com.bioacupunt.copilot.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.copilot.ClinicalCopilotEngine
import com.bioacupunt.copilot.CopilotTool
import com.bioacupunt.copilot.evidence.EvidenceExplorer
import com.bioacupunt.copilot.patient.PatientContextProvider
import com.bioacupunt.copilot.rag.ContextBuilder
import com.bioacupunt.copilot.rag.EvidenceGate
import com.bioacupunt.copilot.rag.GroundedResponseGenerator
import com.bioacupunt.copilot.rag.ResponseValidator
import com.bioacupunt.copilot.retrieval.IntentType
import com.bioacupunt.mtc.knowledge.domain.ClinicalIntelligenceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════

enum class CopilotMode {
    GENERAL,        // No specific context
    PATIENT,        // Patient context active
    KNOWLEDGE,      // Library/knowledge context
    DIFFERENTIAL,   // Differential analysis context
}

enum class CopilotUiState {
    IDLE,
    LOADING,
    SUCCESS,
    NO_EVIDENCE,
    PARTIAL_RESULT,
    PATIENT_CONTEXT_UNAVAILABLE,
    MODEL_UNAVAILABLE,
    OFFLINE,
    ERROR,
}

data class CopilotMessage(
    val role: CopilotRole,
    val text: String,
    val intent: IntentType? = null,
    val gateResult: EvidenceGate.GateResult? = null,
    val validationReport: ResponseValidator.ValidationReport? = null,
    val evidenceChain: EvidenceExplorer.EvidenceChain? = null,
    val differentialResult: ClinicalIntelligenceResult? = null,
    val latencyMs: Long = 0,
    val id: String = java.util.UUID.randomUUID().toString(),
)

enum class CopilotRole { USER, ASSISTANT }

data class CopilotUiStateData(
    val messages: List<CopilotMessage> = listOf(
        CopilotMessage(
            CopilotRole.ASSISTANT,
            "Olá, Dra. Sou o Copiloto Clínico. Posso ajudar com:\n\n" +
                "• **Pesquisar conhecimento** MTC na biblioteca\n" +
                "• **Resumir evolução** de um paciente\n" +
                "• **Explicar diferenciais** entre hipóteses\n" +
                "• **Identificar dados faltantes** na avaliação\n" +
                "• **Explorar evidências** e suas fontes\n\n" +
                "Cada resposta é fundamentada em evidências da biblioteca curada. " +
                "Sem evidência, eu não improviso."
        ),
    ),
    val input: String = "",
    val thinking: Boolean = false,
    val uiState: CopilotUiState = CopilotUiState.IDLE,
    val mode: CopilotMode = CopilotMode.GENERAL,
    val patientName: String? = null,
    val patientId: Long? = null,
    val lastResponse: GroundedResponseGenerator.GroundedResponse? = null,
    val lastGateResult: EvidenceGate.GateResult? = null,
    val lastValidationReport: ResponseValidator.ValidationReport? = null,
    val showEvidenceExplorer: Boolean = false,
    val showDifferentialExplanation: Boolean = false,
    val showMissingData: Boolean = false,
    val isOffline: Boolean = false,
)

// ═══════════════════════════════════════════════════════════════════
// VIEWMODEL
// ═══════════════════════════════════════════════════════════════════

class CopilotViewModel(
    private val engine: ClinicalCopilotEngine,
    private val evidenceExplorer: EvidenceExplorer,
    private val patientContextProvider: PatientContextProvider,
    private val patientId: Long? = null,
    private val patientName: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(CopilotUiStateData(
        mode = if (patientId != null && patientId > 0) CopilotMode.PATIENT else CopilotMode.GENERAL,
        patientName = patientName,
        patientId = patientId,
    ))
    val state: StateFlow<CopilotUiStateData> = _state.asStateFlow()

    init {
        if (patientId != null && patientId > 0) {
            _state.update { it.copy(mode = CopilotMode.PATIENT) }
        }
    }

    fun onInputChanged(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun send(question: String = _state.value.input) {
        if (question.isBlank() || _state.value.thinking) return

        _state.update {
            it.copy(
                messages = it.messages + CopilotMessage(CopilotRole.USER, question),
                input = "",
                thinking = true,
                uiState = CopilotUiState.LOADING,
            )
        }

        viewModelScope.launch {
            try {
                val result = engine.process(
                    query = question,
                    patientId = patientId,
                    activePatientId = patientId,
                    sessionId = "copilot-${System.currentTimeMillis()}",
                )

                val assistantMessage = CopilotMessage(
                    role = CopilotRole.ASSISTANT,
                    text = result.response.answer.ifBlank {
                        "Não foi possível gerar uma resposta."
                    },
                    intent = result.intent,
                    gateResult = result.gateResult,
                    validationReport = result.validationReport,
                    latencyMs = result.latencyMs,
                )

                val newUiState = when {
                    result.gateResult?.decision == EvidenceGate.GateDecision.BLOCK_NO_EVIDENCE ||
                    result.gateResult?.decision == EvidenceGate.GateDecision.BLOCK_INSUFFICIENT_EVIDENCE ->
                        CopilotUiState.NO_EVIDENCE
                    result.response.confidence == "INSUFFICIENT" ->
                        CopilotUiState.NO_EVIDENCE
                    result.response.warnings.contains("MODEL_UNAVAILABLE") ->
                        CopilotUiState.MODEL_UNAVAILABLE
                    result.response.warnings.contains("PATIENT_CONTEXT_UNAVAILABLE") ->
                        CopilotUiState.PATIENT_CONTEXT_UNAVAILABLE
                    result.validationReport?.result == ResponseValidator.ValidationResult.HAS_UNSUPPORTED_CLAIMS ->
                        CopilotUiState.PARTIAL_RESULT
                    else -> CopilotUiState.SUCCESS
                }

                _state.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        thinking = false,
                        uiState = newUiState,
                        lastResponse = result.response,
                        lastGateResult = result.gateResult,
                        lastValidationReport = result.validationReport,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        messages = it.messages + CopilotMessage(
                            CopilotRole.ASSISTANT,
                            "Erro ao processar: ${e.message ?: "desconhecido"}",
                        ),
                        thinking = false,
                        uiState = CopilotUiState.ERROR,
                    )
                }
            }
        }
    }

    fun toggleEvidenceExplorer() {
        _state.update { it.copy(showEvidenceExplorer = !it.showEvidenceExplorer) }
    }

    fun toggleDifferentialExplanation() {
        _state.update { it.copy(showDifferentialExplanation = !it.showDifferentialExplanation) }
    }

    fun toggleMissingData() {
        _state.update { it.copy(showMissingData = !it.showMissingData) }
    }

    fun setMode(mode: CopilotMode) {
        _state.update { it.copy(mode = mode) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FACTORY
// ═══════════════════════════════════════════════════════════════════

class CopilotViewModelFactory(
    private val engine: ClinicalCopilotEngine,
    private val evidenceExplorer: EvidenceExplorer,
    private val patientContextProvider: PatientContextProvider,
    private val patientId: Long? = null,
    private val patientName: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CopilotViewModel(engine, evidenceExplorer, patientContextProvider, patientId, patientName) as T
}
