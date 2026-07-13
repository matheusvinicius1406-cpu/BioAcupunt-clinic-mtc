package com.bioacupunt.prontuario.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.prontuario.domain.model.BaGang
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.PulseFinding
import com.bioacupunt.prontuario.domain.model.PulseReading
import com.bioacupunt.prontuario.domain.model.TongueFinding
import com.bioacupunt.prontuario.domain.model.ZangFuPattern
import com.bioacupunt.prontuario.domain.safety.SafetyVerdict
import com.bioacupunt.prontuario.domain.safety.TreatmentProposal
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    /** Flags actually in force = today's + everything ever recorded. */
    val effectiveFlags: Set<ClinicalFlag> get() = draft.flags + standingFlags
    val completeness: Float get() = draft.completeness
}

class SupremoViewModel(
    private val repository: MtcAssessmentRepository,
    private val patientId: Long,
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
    }

    // -- Chart edits. Each one re-screens, because a newly ticked "pregnancy" must
    //    invalidate a protocol that was legal one keystroke ago.

    fun updateBaGang(baGang: BaGang) = edit { it.copy(baGang = baGang) }

    fun updateTongue(tongue: TongueFinding) = edit { it.copy(tongue = tongue) }

    fun updateChiefComplaint(text: String) = edit { it.copy(chiefComplaint = text) }

    fun updateImpression(text: String) = edit { it.copy(clinicalImpression = text) }

    fun updateGestationalWeeks(weeks: Int?) = edit { it.copy(gestationalWeeks = weeks) }

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

    fun toggleFlag(flag: ClinicalFlag) = edit { draft ->
        val flags = if (flag in draft.flags) draft.flags - flag else draft.flags + flag
        draft.copy(flags = flags)
    }

    fun updateProposal(proposal: TreatmentProposal) {
        _state.update { it.copy(proposal = proposal) }
        rescreenAsync()
    }

    private fun edit(transform: (MtcAssessment) -> MtcAssessment) {
        _state.update { it.copy(draft = transform(it.draft), savedAt = null) }
        rescreenAsync()
    }

    /**
     * Re-runs the deterministic safety screen against the current draft + proposal.
     *
     * Screening is re-run on **every** edit rather than only when the practitioner asks
     * for it. A verdict computed once and left stale is a verdict that says "safe" about
     * a chart that has since changed — and the moment it is most likely to be wrong is
     * exactly the moment a contraindication is being typed in.
     */
    private fun rescreenAsync() = viewModelScope.launch { rescreen() }

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
}

class SupremoViewModelFactory(
    private val repository: MtcAssessmentRepository,
    private val patientId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SupremoViewModel(repository, patientId) as T
}
