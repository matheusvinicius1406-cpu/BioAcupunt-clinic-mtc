package com.bioacupunt.agenda.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.usecase.CalculateDayStats
import com.bioacupunt.agenda.domain.usecase.GetAppointmentsByDate
import com.bioacupunt.agenda.domain.usecase.SaveAppointment
import com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus
import com.bioacupunt.core.util.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgendaUiState(
    val selectedDate: String = todayIso(),
    val appointments: List<com.bioacupunt.agenda.domain.model.Appointment> = emptyList(),
    val stats: Map<String, Any> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AgendaViewModelFactory(
    private val getAppointmentsByDate: GetAppointmentsByDate,
    private val saveAppointment: SaveAppointment,
    private val updateStatus: UpdateAppointmentStatus,
    private val calculateDayStats: CalculateDayStats
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(getAppointmentsByDate, saveAppointment, updateStatus, calculateDayStats) as T
    }
}

class AgendaViewModel(
    private val getAppointmentsByDate: GetAppointmentsByDate,
    private val saveAppointment: SaveAppointment,
    private val updateStatus: UpdateAppointmentStatus,
    private val calculateDayStats: CalculateDayStats
) : ViewModel() {

    private val _state = MutableStateFlow(AgendaUiState())
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    init {
        observeDate(state.value.selectedDate)
    }

    fun onDateSelected(date: String) {
        _state.update { it.copy(selectedDate = date) }
        observeDate(date)
    }

    fun onStatusChange(appointmentId: Long, status: AppointmentStatus) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = updateStatus(appointmentId, status)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _state.update { it.copy(isLoading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createAppointment(patientId: Long, patientName: String, time: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val now = java.time.Instant.now().toString()
            val appointment = com.bioacupunt.agenda.domain.model.Appointment(
                patientId = patientId,
                patientName = patientName,
                date = _state.value.selectedDate,
                time = time,
                notes = "",
                createdAt = now
            )
            val result = saveAppointment(appointment)
            if (result is com.bioacupunt.core.util.Result.Error) {
                _state.update { it.copy(isLoading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun observeDate(date: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, selectedDate = date) }
            getAppointmentsByDate(date)
                .catch { e -> _state.update { it.copy(isLoading = false, error = it.error ?: e.localizedMessage) } }
                .collect { list ->
                    val stats = if (list.isEmpty()) emptyMap() else calculateDayStats(date)
                    _state.update { it.copy(appointments = list, stats = stats, isLoading = false, error = null) }
                }
        }
    }
}

private fun todayIso(): String = java.time.LocalDate.now().toString()
