package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.prontuario.domain.model.BaGang
import com.bioacupunt.prontuario.domain.model.BodyMark
import com.bioacupunt.prontuario.domain.model.BodySide
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.ClinicalSynthesis
import com.bioacupunt.prontuario.domain.model.ConfidenceLevel
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.Organ
import com.bioacupunt.prontuario.domain.model.PatternFactor
import com.bioacupunt.prontuario.domain.model.PulseFinding
import com.bioacupunt.prontuario.domain.model.PulseReading
import com.bioacupunt.prontuario.domain.model.TongueFinding
import com.bioacupunt.prontuario.domain.model.ZangFuPattern
import com.bioacupunt.prontuario.domain.safety.BodyRegion
import com.bioacupunt.prontuario.domain.safety.SafetyVerdict
import com.bioacupunt.prontuario.domain.safety.Technique
import com.bioacupunt.prontuario.domain.safety.TreatmentProposal
import com.bioacupunt.prontuario.domain.usecase.ChiefComplaintExtraction
import com.bioacupunt.prontuario.domain.usecase.ClinicalSynthesisUseCase
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import com.bioacupunt.prontuario.domain.usecase.StructureChiefComplaintUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SupremoUiState(
    val patientId: Long = 0,
    val draft: MtcAssessment = MtcAssessment(patientId = 0),
    val history: List<MtcAssessment> = emptyList(),
    /** Flags carried over from previous sessions. Shown as read-only context. */
    val standingFlags: Set<ClinicalFlag> = emptySet(),
    val proposal: TreatmentProposal = TreatmentProposal(),
    val verdict: SafetyVerdict = SafetyVerdict(emptyList()),
    val saving: Boolean = false,
    val savedAt: String? = null,
    val error: String? = null,
    /**
     * Sugestão extrativa da IA sobre o texto de [MtcAssessment.chiefComplaint] já
     * escrito — nunca gravada sozinha, só existe pra médica aceitar/ignorar item a
     * item. Null = nenhuma sugestão pendente (nada extraído, ou tudo já aceito/
     * descartado).
     */
    val chiefComplaintSuggestion: ChiefComplaintExtraction? = null,
    /**
     * SÍNTESE CLÍNICA COMPLETA — IA analisa TODO o prontuário e sugere diagnóstico
     * MTC + biomédico + plano terapêutico. A médica revisa, edita e decide o que
     * aceitar. Null = nenhuma síntese foi gerada ainda.
     */
    val clinicalSynthesis: ClinicalSynthesis? = null,
    val synthesizing: Boolean = false,
    val synthesisError: String? = null,
) {
    /** Flags actually in force = today's + everything ever recorded. */
    val effectiveFlags: Set<ClinicalFlag> get() = draft.flags + standingFlags
    val completeness: Float get() = draft.completeness
}

@OptIn(FlowPreview::class)
class SupremoViewModel(
    private val repository: MtcAssessmentRepository,
    private val patientId: Long,
    private val structureChiefComplaint: StructureChiefComplaintUseCase? = null,
    private val clinicalSynthesisUseCase: ClinicalSynthesisUseCase? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SupremoUiState(
            patientId = patientId,
            draft = MtcAssessment(
                patientId = patientId,
                date = LocalDate.now().toString(),
            ),
        ),
    )
    val state: StateFlow<SupremoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.standingFlags(patientId) }
                .onSuccess { flags -> _state.update { it.copy(standingFlags = flags) } }
            rescreen()
        }
        viewModelScope.launch {
            repository.observeHistory(patientId).collect { history ->
                _state.update { it.copy(history = history) }
            }
        }
        // Re-triagem reativa e debounced: toda edição de draft/proposal precisa re-rodar
        // a triagem (um veredito computado uma vez e envelhecido diz "seguro" sobre um
        // prontuário que já mudou — ver rescreen()), mas antes cada `edit{}`/
        // `updateProposal` disparava `rescreenAsync()` synchronously, e cada um deles
        // fazia 2 queries no Room (`flagsHistory`/`latestGestationalWeeks`) — uma rajada
        // por tecla digitada em Motivo da Consulta, Plano ou notas de língua/pulso.
        // `distinctUntilChanged` + `debounce` + `collectLatest` é o mesmo padrão já usado
        // abaixo para a sugestão extrativa de IA: só a QUERY é adiada, o dado em si
        // (`_state.draft`) já está atualizado na tela desde o primeiro `_state.update`.
        viewModelScope.launch {
            _state
                .map { it.draft to it.proposal }
                .distinctUntilChanged()
                .debounce(RESCREEN_DEBOUNCE_MS)
                .collectLatest { rescreen() }
        }
        if (structureChiefComplaint != null) {
            viewModelScope.launch {
                _state
                    .map { it.draft.chiefComplaint }
                    .distinctUntilChanged()
                    .debounce(CHIEF_COMPLAINT_DEBOUNCE_MS)
                    .collectLatest { text ->
                        val extraction = structureChiefComplaint(text)
                        _state.update {
                            it.copy(chiefComplaintSuggestion = extraction.takeUnless { e -> e.isEmpty })
                        }
                    }
            }
        }
    }

    // -- Chart edits. Each one re-screens, because a newly ticked "pregnancy" must
    //    invalidate a protocol that was legal one keystroke ago.

    fun updateBaGang(baGang: BaGang) = edit { it.copy(baGang = baGang) }

    fun updateTongue(tongue: TongueFinding) = edit { it.copy(tongue = tongue) }

    fun updateChiefComplaint(text: String) = edit { it.copy(chiefComplaint = text) }

    fun updateImpression(text: String) = edit { it.copy(clinicalImpression = text) }

    fun updateGestationalWeeks(weeks: Int?) = edit { it.copy(gestationalWeeks = weeks) }

    /**
     * Records a clinical override: the practitioner chose to proceed despite a
     * FORBIDDEN safety verdict. The reason, user identity and timestamp are
     * stored in the assessment for audit (LGPD/CFM-compatible).
     *
     * [userId] is passed from the UI layer (AuthRepository or SecurePreferences);
     * it is intentionally not stored in the ViewModel so the practitioner's
     * identity is explicit at the call site rather than hidden in DI.
     */
    fun overrideVeto(reason: String, userId: String) {
        if (reason.trim().length < 10) return
        val now = java.time.Instant.now().toString()
        _state.update {
            it.copy(
                draft = it.draft.copy(
                    overrideReason = reason.trim(),
                    overrideBy = userId,
                    overrideAt = now,
                ),
            )
        }
        // Um override é um EVENTO DE AUDITORIA (LGPD/CFM): quem prosseguiu sobre um
        // veredito FORBIDDEN, quando e por quê. Manter isso só no draft em memória
        // perde o registro se a médica sair do prontuário sem disparar outro save.
        // `save()` lê _state.value.draft — que a atualização síncrona acima já
        // preencheu com os campos de override —, então persiste na hora.
        save()
    }

    /** Anota um padrão Zang-Fu por texto livre — cria a linha (sem fatores) se ainda não houver uma pro órgão. */
    fun updatePatternNotes(organ: Organ, text: String) = edit { draft ->
        val existing = draft.patterns.firstOrNull { it.organ == organ }
        val updated = (existing ?: ZangFuPattern(organ = organ)).copy(notes = text)
        draft.copy(patterns = draft.patterns.filterNot { it.organ == organ } + updated)
    }

    fun togglePattern(pattern: ZangFuPattern) = edit { draft ->
        val existing = draft.patterns.firstOrNull { it.organ == pattern.organ }
        val patterns = if (existing != null) {
            draft.patterns - existing
        } else {
            draft.patterns + pattern
        }
        draft.copy(patterns = patterns)
    }

    fun setPulseReading(reading: PulseReading) = edit { draft ->
        val others = draft.pulse.readings.filterNot {
            it.wrist == reading.wrist &&
                it.position == reading.position &&
                it.depth == reading.depth
        }
        val readings = if (reading.qualities.isEmpty()) others else others + reading
        draft.copy(pulse = draft.pulse.copy(readings = readings))
    }

    fun updatePulseRate(bpm: Int?) = edit { it.copy(pulse = it.pulse.copy(rateBpm = bpm)) }

    fun updatePulseNotes(text: String) = edit { it.copy(pulse = it.pulse.copy(notes = text)) }

    fun updateTongueNotes(text: String) = edit { it.copy(tongue = it.tongue.copy(notes = text)) }

    fun updateTonguePhoto(uri: String?) = edit { it.copy(tongue = it.tongue.copy(photoUri = uri)) }

    fun toggleFlag(flag: ClinicalFlag) = edit { draft ->
        val flags = if (flag in draft.flags) draft.flags - flag else draft.flags + flag
        draft.copy(flags = flags)
    }

    fun updateProposal(proposal: TreatmentProposal) {
        _state.update { it.copy(proposal = proposal) }
    }

    fun toggleTechnique(technique: Technique) {
        val current = _state.value.proposal
        val techniques = if (technique in current.techniques) current.techniques - technique else current.techniques + technique
        updateProposal(current.copy(techniques = techniques))
    }

    // -- Atendimento wizard: step 1 (mapa corporal, melhora/piora) --------

    fun toggleRelieving(factor: String) = edit { draft ->
        val set = if (factor in draft.relievingFactors) draft.relievingFactors - factor else draft.relievingFactors + factor
        draft.copy(relievingFactors = set)
    }

    fun toggleAggravating(factor: String) = edit { draft ->
        val set = if (factor in draft.aggravatingFactors) draft.aggravatingFactors - factor else draft.aggravatingFactors + factor
        draft.copy(aggravatingFactors = set)
    }

    /** Sets (or clears, at eva=0) the pain level for a body region — one mark per region. */
    fun setRegionEva(region: BodyRegion, eva: Int) = edit { draft ->
        val others = draft.bodyMarks.filterNot { it.region == region }
        val marks = if (eva <= 0) others else others + BodyMark(
            id = region.name,
            side = BodySide.ANTERIOR,
            x = 0f, y = 0f,
            intensity = eva.coerceIn(0, 10),
            region = region,
        )
        draft.copy(bodyMarks = marks)
    }

    // -- Atendimento wizard: step 2 (interrogatório) -----------------------

    fun toggleReviewOfSystems(item: String) = edit { draft ->
        val set = if (item in draft.reviewOfSystems) draft.reviewOfSystems - item else draft.reviewOfSystems + item
        draft.copy(reviewOfSystems = set)
    }

    fun updateInterrogationNotes(text: String) = edit { it.copy(interrogationNotes = text) }

    // -- Sugestão extrativa da IA sobre o Motivo da Consulta ---------------
    //
    // Aceitar um item chama a MESMA função que um toque manual chamaria — não há
    // segundo caminho de escrita. Aceitar também remove aquele item específico da
    // sugestão pendente, pra o chip sumir assim que usado.

    fun acceptAggravatingSuggestion(item: String) {
        toggleAggravating(item)
        pruneSuggestion { it.copy(aggravating = it.aggravating - item) }
    }

    fun acceptRelievingSuggestion(item: String) {
        toggleRelieving(item)
        pruneSuggestion { it.copy(relieving = it.relieving - item) }
    }

    fun acceptReviewOfSystemsSuggestion(item: String) {
        toggleReviewOfSystems(item)
        pruneSuggestion { it.copy(reviewOfSystemsHits = it.reviewOfSystemsHits - item) }
    }

    fun dismissChiefComplaintSuggestion() = _state.update { it.copy(chiefComplaintSuggestion = null) }

    private fun pruneSuggestion(transform: (ChiefComplaintExtraction) -> ChiefComplaintExtraction) {
        _state.update { state ->
            val updated = state.chiefComplaintSuggestion?.let(transform)
            state.copy(chiefComplaintSuggestion = updated?.takeUnless { it.isEmpty })
        }
    }

    // -- Atendimento wizard: step 3 (Zang Fu cycling) ----------------------

    /** Cycles an organ through Normal -> Deficiência -> Estagnação -> Normal. */
    fun cycleOrganState(organ: Organ) = edit { draft ->
        val existing = draft.patterns.firstOrNull { it.organ == organ }
        val next = when {
            existing == null -> ZangFuPattern(organ = organ, factors = setOf(PatternFactor.QI_DEFICIENCY))
            PatternFactor.QI_DEFICIENCY in existing.factors -> existing.copy(factors = setOf(PatternFactor.QI_STAGNATION))
            PatternFactor.QI_STAGNATION in existing.factors -> null
            else -> ZangFuPattern(organ = organ, factors = setOf(PatternFactor.QI_DEFICIENCY))
        }
        val patterns = draft.patterns.filterNot { it.organ == organ } + listOfNotNull(next)
        draft.copy(patterns = patterns)
    }

    /** Normal / Deficiência / Estagnação label for the current organ state — UI-facing. */
    fun organStateLabel(organ: Organ): String {
        val existing = _state.value.draft.patterns.firstOrNull { it.organ == organ } ?: return "Normal"
        return when {
            PatternFactor.QI_DEFICIENCY in existing.factors -> "Deficiência"
            PatternFactor.QI_STAGNATION in existing.factors -> "Estagnação"
            else -> "Normal"
        }
    }

    // -- Atendimento wizard: step 5 (orientações) --------------------------

    fun updateOrientations(text: String) = edit { it.copy(orientations = text) }

    private fun edit(transform: (MtcAssessment) -> MtcAssessment) {
        _state.update { it.copy(draft = transform(it.draft), savedAt = null) }
    }

    /**
     * Re-runs the deterministic safety screen against the current draft + proposal.
     *
     * Screening is re-run on **every** edit (via the debounced collector in `init`)
     * rather than only when the practitioner asks for it. A verdict computed once and
     * left stale is a verdict that says "safe" about a chart that has since changed —
     * and the moment it is most likely to be wrong is exactly the moment a
     * contraindication is being typed in.
     */
    private suspend fun rescreen() {
        val snapshot = _state.value
        runCatching {
            repository.screen(
                patientId = patientId,
                proposal = snapshot.proposal,
                draft = snapshot.draft,
            )
        }.onSuccess { verdict ->
            _state.update { it.copy(verdict = verdict, error = null) }
        }.onFailure { error ->
            // Fail *loud*, never silently "clear". A screen that errored is not a
            // patient who is safe.
            _state.update {
                it.copy(error = "Falha na triagem de segurança: ${error.message}")
            }
        }
    }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null) }
        val now = java.time.Instant.now().toString()
        val draft = _state.value.draft.let {
            it.copy(
                createdAt = it.createdAt.ifBlank { now },
                updatedAt = now,
            )
        }
        runCatching { repository.save(draft) }
            .onSuccess { id ->
                _state.update {
                    it.copy(
                        draft = draft.copy(id = id),
                        saving = false,
                        savedAt = now,
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(saving = false, error = "Não foi possível salvar: ${error.message}")
                }
            }
    }

    // ── Clinical Synthesis ─────────────────────────────────────────────
    //
    // A IA analisa TODO o prontuário deste paciente e gera uma sugestão
    // diagnóstica + plano. A médica revisa, edita, aceita ou descarta.
    // NUNCA salva automaticamente.

    /**
     * Gera a síntese clínica chamando a IA com todos os dados do prontuário:
     * MtcAssessment completo + histórico + dados de exames.
     */
    fun synthesizeDiagnosis(
        labSummary: String = "",
        activeMedications: String = "",
        allergySummary: String = "",
    ) {
        if (_state.value.synthesizing) return
        if (clinicalSynthesisUseCase == null) {
            _state.update { it.copy(synthesisError = "IA de síntese não configurada") }
            return
        }
        _state.update { it.copy(synthesizing = true, synthesisError = null) }
        viewModelScope.launch {
            val snapshot = _state.value
            val result = clinicalSynthesisUseCase(
                assessment = snapshot.draft,
                history = snapshot.history,
                labSummary = labSummary,
                activeMedications = activeMedications,
                allergySummary = allergySummary,
            )
            _state.update {
                it.copy(
                    clinicalSynthesis = result.takeUnless { syn -> syn.isEmpty },
                    synthesizing = false,
                    synthesisError = null,
                )
            }
        }
    }

    /**
     * A médica ACEITOU o diagnóstico MTC sugerido pela IA — faz append no campo
     * clinicalImpression para não sobrescrever o que ela já escreveu.
     */
    fun acceptTcmSynthesis() {
        val synthesis = _state.value.clinicalSynthesis?.tcmDiagnosis ?: return
        val current = _state.value.draft.clinicalImpression
        val suffix = buildString {
            appendLine("\n\nSugestão da IA — padrão MTC:")
            appendLine(synthesis.patternName)
            if (synthesis.organInvolvement.isNotEmpty()) {
                appendLine("Órgãos: ${synthesis.organInvolvement.joinToString(", ")}")
            }
            if (synthesis.baGangClassification.isNotBlank()) {
                appendLine("Ba Gang: ${synthesis.baGangClassification}")
            }
            if (synthesis.explanation.isNotBlank()) {
                appendLine(synthesis.explanation)
            }
        }
        updateImpression(current.trim() + suffix)
    }

    /**
     * A médica ACEITOU o diagnóstico biomédico sugerido pela IA.
     */
    fun acceptBiomedicalSynthesis() {
        val synthesis = _state.value.clinicalSynthesis?.biomedicalDiagnosis ?: return
        val current = _state.value.draft.clinicalImpression
        val suffix = buildString {
            appendLine("\n\nDiagnóstico biomédico (sugestão IA):")
            appendLine(synthesis.diagnosis)
            if (synthesis.cidCode.isNotBlank()) appendLine("CID-10: ${synthesis.cidCode}")
            if (synthesis.cid11Code.isNotBlank()) appendLine("CID-11: ${synthesis.cid11Code}")
            if (synthesis.explanation.isNotBlank()) appendLine(synthesis.explanation)
        }
        updateImpression(current.trim() + suffix)
    }

    /**
     * A médica ACEITOU o plano terapêutico sugerido pela IA — leva para
     * [MtcAssessment.orientations].
     */
    fun acceptTherapeuticSynthesis() {
        val therapy = _state.value.clinicalSynthesis?.therapeuticSuggestion ?: return
        val text = buildString {
            if (therapy.objectives.isNotBlank()) {
                appendLine("Objetivos: ${therapy.objectives}")
                appendLine()
            }
            if (therapy.recommendedTechniques.isNotEmpty()) {
                appendLine("Técnicas: ${therapy.recommendedTechniques.joinToString(", ")}")
            }
            if (therapy.acupuncturePoints.isNotEmpty()) {
                appendLine("Pontos: ${therapy.acupuncturePoints.joinToString(", ")}")
            }
            if (therapy.pointCombinations.isNotEmpty()) {
                appendLine("Combinações: ${therapy.pointCombinations.joinToString(", ")}")
            }
            if (therapy.cautionAndContraindications.isNotEmpty()) {
                appendLine("Cuidados: ${therapy.cautionAndContraindications.joinToString("; ")}")
            }
            therapy.sessionCount?.let { appendLine("Sessões sugeridas: $it") }
            if (therapy.frequency.isNotBlank()) appendLine("Frequência: ${therapy.frequency}")
        }
        updateOrientations(text)
    }

    /** Descartar a síntese atual — a médica não quer usar nada. */
    fun dismissSynthesis() {
        _state.update { it.copy(clinicalSynthesis = null, synthesisError = null) }
    }

    /** Descartar erro de síntese. */
    fun clearSynthesisError() {
        _state.update { it.copy(synthesisError = null) }
    }

    private companion object {
        const val CHIEF_COMPLAINT_DEBOUNCE_MS = 1200L
        const val RESCREEN_DEBOUNCE_MS = 400L
    }
}

class SupremoViewModelFactory(
    private val repository: MtcAssessmentRepository,
    private val patientId: Long,
    private val structureChiefComplaint: StructureChiefComplaintUseCase? = null,
    private val clinicalSynthesisUseCase: ClinicalSynthesisUseCase? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SupremoViewModel(repository, patientId, structureChiefComplaint, clinicalSynthesisUseCase) as T
}
