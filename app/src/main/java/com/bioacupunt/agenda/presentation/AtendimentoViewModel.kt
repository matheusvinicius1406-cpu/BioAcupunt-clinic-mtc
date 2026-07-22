package com.bioacupunt.agenda.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus
import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.model.ProntuarioEntryType
import com.bioacupunt.prontuario.domain.usecase.AddEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AtendimentoUiState(
    val appointment: Appointment? = null,
    val loading: Boolean = true,
    val finalizing: Boolean = false,
    val finalized: Boolean = false,
    val error: String? = null,
)

/** Orchestrates the Atendimento wizard's "Finalizar" action: closes out the real
 * appointment and files a real evolution entry. The MTC chart itself (Ba Gang/Zang
 * Fu/tongue/pulse/safety) is [com.bioacupunt.prontuario.presentation.SupremoViewModel]'s
 * job — this ViewModel only knows about the appointment being attended. */
class AtendimentoViewModel(
    private val appointmentRepository: AppointmentRepository,
    private val updateAppointmentStatus: UpdateAppointmentStatus,
    private val addEntry: AddEntry,
    private val appointmentId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(AtendimentoUiState())
    val state: StateFlow<AtendimentoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = appointmentRepository.getById(appointmentId)) {
                is Result.Success -> _state.update {it.copy(appointment = result.data, loading = false) }
                is Result.Error -> _state.update {it.copy(error = result.kind.userMessage, loading = false) }
                is Result.Loading -> Unit
            }
        }
    }

    fun finalize(summary: String, onDone: () -> Unit) {
        val appt = _state.value.appointment ?: return
        viewModelScope.launch {
            _state.update {it.copy(finalizing = true, error = null) }
            val statusResult = updateAppointmentStatus(appt.id, AppointmentStatus.COMPLETED)
            if (statusResult is Result.Error) {
                _state.update {it.copy(finalizing = false, error = statusResult.kind.userMessage) }
                return@launch
            }
            val now = java.time.Instant.now().toString()
            val entryResult = addEntry(
                ProntuarioEntry(
                    patientId = appt.patientId,
                    doctorName = "",
                    date = now,
                    type = ProntuarioEntryType.EVOLUTION,
                    body = summary.ifBlank { "Sessão ${appt.sessionNumber} — ${appt.type}" },
                    createdAt = now,
                    updatedAt = now,
                )
            )
            if (entryResult is Result.Error) {
                // The appointment status was already updated, but the evolution note
                // itself did not save. Surface this loudly instead of reporting
                // "finalized" over a session that has no documentation — a doctor who
                // sees success and navigates away would have no reason to retry.
                _state.update { it.copy(finalizing = false, error = entryResult.kind.userMessage) }
                return@launch
            }
            _state.update {it.copy(finalizing = false, finalized = true) }
            onDone()
        }
    }

}

class AtendimentoViewModelFactory(
    private val appointmentRepository: AppointmentRepository,
    private val updateAppointmentStatus: UpdateAppointmentStatus,
    private val addEntry: AddEntry,
    private val appointmentId: Long,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AtendimentoViewModel(appointmentRepository, updateAppointmentStatus, addEntry, appointmentId) as T
    }
}
