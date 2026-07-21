package com.bioacupunt.agenda.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.agenda.domain.model.AppointmentStatus
import com.bioacupunt.agenda.domain.usecase.CalculateDayStats
import com.bioacupunt.agenda.domain.usecase.GetAppointmentsByDate
import com.bioacupunt.agenda.domain.usecase.GetAppointmentsInRange
import com.bioacupunt.agenda.domain.usecase.SaveAppointment
import com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus
import com.bioacupunt.core.util.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class AgendaUiState(
    val selectedDate: String = todayIso(),
    val visibleMonth: String = YearMonth.now().toString(),
    val appointments: List<com.bioacupunt.agenda.domain.model.Appointment> = emptyList(),
    val monthAppointments: List<com.bioacupunt.agenda.domain.model.Appointment> = emptyList(),
    val showFreeSlots: Boolean = false,
    val stats: Map<String, Any> = emptyMap(),
    /** The patients an appointment can be booked for. Empty until the CRM loads. */
    val patients: List<com.bioacupunt.crm.domain.model.CrmPatient> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AgendaViewModelFactory(
    private val getAppointmentsByDate: GetAppointmentsByDate,
    private val getAppointmentsInRange: GetAppointmentsInRange,
    private val saveAppointment: SaveAppointment,
    private val updateStatus: UpdateAppointmentStatus,
    private val calculateDayStats: CalculateDayStats,
    private val crmPatientRepository: com.bioacupunt.crm.domain.repository.CrmPatientRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AgendaViewModel(
            getAppointmentsByDate, getAppointmentsInRange, saveAppointment,
            updateStatus, calculateDayStats, crmPatientRepository
        ) as T
    }
}

class AgendaViewModel(
    private val getAppointmentsByDate: GetAppointmentsByDate,
    private val getAppointmentsInRange: GetAppointmentsInRange,
    private val saveAppointment: SaveAppointment,
    private val updateStatus: UpdateAppointmentStatus,
    private val calculateDayStats: CalculateDayStats,
    private val crmPatientRepository: com.bioacupunt.crm.domain.repository.CrmPatientRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(AgendaUiState())
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    init {
        observeDate(state.value.selectedDate)
        observeMonth(YearMonth.parse(state.value.visibleMonth))
        observePatients()
    }

    private fun observePatients() {
        val repository = crmPatientRepository ?: return
        viewModelScope.launch {
            repository.observeAll()
                .catch { /* the picker simply stays empty; booking is blocked, not crashed */ }
                .collect { list -> _state.update { it.copy(patients = list) } }
        }
    }

    fun onDateSelected(date: String) {
        _state.update { it.copy(selectedDate = date) }
        observeDate(date)
        val month = YearMonth.from(java.time.LocalDate.parse(date))
        if (month.toString() != _state.value.visibleMonth) onMonthChanged(month)
    }

    fun onMonthChanged(month: YearMonth) {
        _state.update { it.copy(visibleMonth = month.toString()) }
        observeMonth(month)
    }

    fun toggleFreeSlots() {
        _state.update { it.copy(showFreeSlots = !it.showFreeSlots) }
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

    fun createAppointment(
        patientId: Long,
        patientName: String,
        time: String,
        valueBrl: Double,
        type: com.bioacupunt.agenda.domain.model.AppointmentType
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val now = java.time.Instant.now().toString()
            val appointment = com.bioacupunt.agenda.domain.model.Appointment(
                patientId = patientId,
                patientName = patientName,
                date = _state.value.selectedDate,
                time = time,
                type = type.name,
                valueBrl = valueBrl,
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

    private fun observeMonth(month: YearMonth) {
        viewModelScope.launch {
            val start = month.atDay(1).toString()
            val end = month.atEndOfMonth().toString()
            getAppointmentsInRange(start, end)
                .catch { emit(emptyList()) }
                .collect { list -> _state.update { it.copy(monthAppointments = list) } }
        }
    }
}

private fun todayIso(): String = java.time.LocalDate.now().toString()
