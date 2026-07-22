package com.bioacupunt.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.crm.domain.usecase.UpdateCrmStage
import com.bioacupunt.core.util.Result
import com.bioacupunt.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Severity of a dashboard insight. Maps to icon/colour in the UI layer only. */
enum class DashInsightLevel { ALERT, INFO, POSITIVE }

/**
 * A dashboard insight computed **deterministically** from real CRM/agenda data.
 * There is deliberately no LLM here: showing model-generated text as clinic
 * "insights" would present unreviewed content as if it were a fact about the
 * doctor's own patients. Everything below is arithmetic over the database.
 */
data class DashInsight(
    val level: DashInsightLevel,
    val title: String,
    val desc: String,
)

/** A patient overdue for a return visit — backs the "Reengajamento" widget. */
data class ReengagePatient(
    val patientId: Long,
    val name: String,
    val initials: String,
    val lastVisitLabel: String,
    val phone: String,
)

/** One clinical-pipeline card — backs a column of the "Kanban Clínico" widget. */
data class KanbanCard(
    val patientId: Long,
    val name: String,
    val initials: String,
    val note: String,
)

data class KanbanColumn(
    val stage: PatientStage,
    val count: Int,
    val cards: List<KanbanCard>,
)

data class DashboardUiState(
    val greetingName: String = "",        // blank -> greet without a name
    val todayCount: Int = 0,
    val activeCount: Int = 0,
    val totalPatients: Int = 0,
    val overdueCount: Int = 0,
    val noNextCount: Int = 0,
    val todayAppointments: List<Appointment> = emptyList(),
    val nextAppointment: Appointment? = null,
    val monthReceivedBrl: Double = 0.0,
    val monthPendingBrl: Double = 0.0,
    val insights: List<DashInsight> = emptyList(),
    val reengage: List<ReengagePatient> = emptyList(),
    val kanban: List<KanbanColumn> = emptyList(),
    val isLoading: Boolean = true,
)

class DashboardViewModelFactory(
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val crmPatientRepository: CrmPatientRepository,
    private val transacaoRepository: TransacaoRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(
            authRepository,
            appointmentRepository,
            crmPatientRepository,
            transacaoRepository,
        ) as T
    }
}

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val crmPatientRepository: CrmPatientRepository,
    private val transacaoRepository: TransacaoRepository,
    private val today: LocalDate = LocalDate.now(),
    private val now: LocalTime = LocalTime.now(),
) : ViewModel() {

    private val updateCrmStage = UpdateCrmStage(crmPatientRepository)

    private val _state = MutableStateFlow(DashboardUiState(isLoading = true))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(greetingName = authRepository.getCurrentUser()?.name.orEmpty()) }
        observeCoreData()
        observeNextAppointment()
        refreshFinance()
    }

    /** Reload month totals (they are point-in-time sums, not observable streams). */
    fun refreshFinance(tenantId: Long = 1L) {
        viewModelScope.launch {
            val start = today.withDayOfMonth(1).toString()
            val end = today.toString()
            val received = (transacaoRepository.sumPayments(tenantId, start, end) as? Result.Success)?.data ?: 0.0
            val pending = (transacaoRepository.sumPending(tenantId, start, end) as? Result.Success)?.data ?: 0.0
            _state.update { it.copy(monthReceivedBrl = received, monthPendingBrl = pending) }
        }
    }

    /** Advance a patient to the next stage of the clinical pipeline (Kanban card tap). */
    fun advanceStage(patientId: Long) {
        viewModelScope.launch {
            val current = crmPatientRepository.getById(patientId)
            if (current !is Result.Success) return@launch
            val stage = runCatching { PatientStage.valueOf(current.data.stage) }.getOrDefault(PatientStage.FIRST_CONTACT)
            val stages = PatientStage.entries
            val next = stages.getOrNull(stage.ordinal + 1) ?: return@launch
            updateCrmStage(patientId, next)
        }
    }

    private fun observeCoreData() {
        viewModelScope.launch {
            combine(
                appointmentRepository.observeByDate(today.toString()),
                crmPatientRepository.observeAll(),
            ) { appts, patients -> appts to patients }
                .catch { _state.update { it.copy(isLoading = false) } }
                .collect { (appts, patients) ->
                    val active = patients.filter { it.stage in ACTIVE_STAGES }
                    val overdue = overduePatients(active)
                    val noNext = active.filter { it.nextAppointment.isBlank() }
                    _state.update {
                        it.copy(
                            todayAppointments = appts.sortedBy { a -> a.time },
                            todayCount = appts.size,
                            activeCount = active.size,
                            totalPatients = patients.size,
                            overdueCount = overdue.size,
                            noNextCount = noNext.size,
                            insights = computeInsights(overdue, noNext),
                            reengage = overdue.take(5).map { (p, days) ->
                                ReengagePatient(
                                    patientId = p.id,
                                    name = p.name,
                                    initials = initialsOf(p.name),
                                    lastVisitLabel = "há $days dias",
                                    phone = p.phone,
                                )
                            },
                            kanban = buildKanban(patients),
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun observeNextAppointment() {
        viewModelScope.launch {
            appointmentRepository.observeNextUpcoming(today.toString(), now.format(TIME_FORMATTER))
                .catch { emit(null) }
                .collect { next -> _state.update { it.copy(nextAppointment = next) } }
        }
    }

    private fun overduePatients(active: List<CrmPatient>): List<Pair<CrmPatient, Long>> {
        return active
            .mapNotNull { p -> parseVisitDate(p.lastVisit)?.let { p to ChronoUnit.DAYS.between(it, today) } }
            .filter { it.second >= RETURN_OVERDUE_DAYS }
            .sortedByDescending { it.second }
    }

    private fun buildKanban(patients: List<CrmPatient>): List<KanbanColumn> {
        val byStage = patients.groupBy { it.stage }
        return PatientStage.entries.map { stage ->
            val inStage = byStage[stage.name].orEmpty()
            KanbanColumn(
                stage = stage,
                count = inStage.size,
                cards = inStage.take(3).map { p ->
                    KanbanCard(
                        patientId = p.id,
                        name = p.name,
                        initials = initialsOf(p.name),
                        note = p.mainComplaint.ifBlank { "${p.totalSessions} sessões" },
                    )
                },
            )
        }
    }

    private fun computeInsights(overdue: List<Pair<CrmPatient, Long>>, noNext: List<CrmPatient>): List<DashInsight> {
        val insights = mutableListOf<DashInsight>()
        if (overdue.isNotEmpty()) {
            val names = overdue.take(3).joinToString(", ") { it.first.name.substringBefore(' ') }
            insights += DashInsight(
                DashInsightLevel.ALERT,
                "${overdue.size} sem retorno há ${RETURN_OVERDUE_DAYS}+ dias",
                "$names — considere contato para reagendar.",
            )
        }
        if (noNext.isNotEmpty()) {
            insights += DashInsight(
                DashInsightLevel.INFO,
                "${noNext.size} sem próxima consulta agendada",
                "Pacientes ativos sem retorno marcado.",
            )
        }
        if (insights.isEmpty()) {
            insights += DashInsight(
                DashInsightLevel.POSITIVE,
                "Tudo em dia",
                "Nenhum paciente ativo em atraso de retorno.",
            )
        }
        return insights
    }

    /** lastVisit is stored as an ISO instant by the seeder, but may be a plain date. Degrade to null. */
    private fun parseVisitDate(raw: String): LocalDate? {
        if (raw.isBlank()) return null
        return runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate() }
            .getOrElse { runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() }
    }

    private fun initialsOf(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private const val RETURN_OVERDUE_DAYS = 30L
        private val ACTIVE_STAGES = setOf(
            PatientStage.ACTIVE.name,
            PatientStage.TREATMENT.name,
            PatientStage.MAINTENANCE.name,
        )
    }
}
