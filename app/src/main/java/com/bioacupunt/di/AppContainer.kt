package com.bioacupunt.di

import android.content.Context
import com.bioacupunt.auth.data.local.AuthRepositoryImpl
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.cache.AppCacheManager
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.data.repository.KnowledgeRepository
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.patient.domain.usecase.CreatePatient
import com.bioacupunt.patient.domain.usecase.GetPatients
import com.bioacupunt.patient.presentation.PatientsViewModelFactory
import com.bioacupunt.patient.presentation.ProntuarioViewModelFactory
import com.bioacupunt.security.SecurePreferences
import com.bioacupunt.sync.SyncScheduler
import com.bioacupunt.core.domain.AppState
import com.bioacupunt.core.domain.AuthState
import com.bioacupunt.core.domain.AIState
import com.bioacupunt.core.domain.NetworkState
import com.bioacupunt.core.domain.SyncState
import com.bioacupunt.core.domain.ThemeState
import com.bioacupunt.core.domain.UserState
import com.bioacupunt.core.domain.SettingsState
import com.bioacupunt.sync.SyncWorkerFactory
import com.bioacupunt.sync.data.local.SyncQueueDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manual DI container. Single source of truth for all dependencies.
 * Thread-safe via @Volatile + double-checked lazy init.
 */
object AppContainer {

    @Volatile private var _context: Context? = null
    private val appContext get() = checkNotNull(_context) { "AppContainer not initialized" }

    fun init(context: Context) {
        if (_context == null) synchronized(this) {
            if (_context == null) _context = context.applicationContext
        }
    }

    // ── Global App State ───────────────────────────────────
    private val _appState = MutableStateFlow<AppState>(AppState.Unknown)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    fun updateAppState(transform: (AppState.Ready) -> AppState.Ready) {
        val current = _appState.value
        if (current is AppState.Ready) {
            _appState.value = transform(current)
        }
    }

    fun initStateIfNeeded() {
        if (_appState.value !is AppState.Ready) {
            _appState.value = AppState.Ready(
                auth = AuthState(),
                sync = SyncState(),
                network = NetworkState(),
                theme = ThemeState(),
                settings = SettingsState(),
                user = UserState(),
                ai = AIState()
            )
        }
    }

    // ── Security ───────────────────────────────────────────
    val securePreferences: SecurePreferences by lazy { SecurePreferences(appContext) }

    // ── Cache ──────────────────────────────────────────────
    val cacheManager: AppCacheManager by lazy { AppCacheManager.getInstance(appContext) }

    // ── Core ───────────────────────────────────────────────
    val appEventManager: com.bioacupunt.core.util.AppEventManager by lazy { com.bioacupunt.core.util.AppEventManager }

    // ── Auth ───────────────────────────────────────────────
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(securePreferences) }

    // ── Database ───────────────────────────────────────────
    val database: AppDatabase by lazy { DatabaseModule.provideAppDatabase(appContext) }

    // ── DAOs ───────────────────────────────────────────────
    val patientDao: PatientDao by lazy { database.patientDao() }
    val syncQueueDao: SyncQueueDao by lazy { database.syncQueueDao() }
    val knowledgeNodeDao: KnowledgeNodeDao by lazy { database.knowledgeNodeDao() }
    val crmPatientDao: com.bioacupunt.crm.data.local.CrmPatientDao by lazy { database.crmPatientDao() }
    val appointmentDao: com.bioacupunt.agenda.data.local.AppointmentDao by lazy { database.appointmentDao() }
    val transacaoDao: com.bioacupunt.financeiro.data.local.TransacaoDao by lazy { database.transacaoDao() }
    val prontuarioDao: com.bioacupunt.prontuario.data.local.ProntuarioDao by lazy { database.prontuarioDao() }
    val bibliotecaDao: com.bioacupunt.biblioteca.data.local.BibliotecaDao by lazy { database.bibliotecaDao() }

    // ── Financeiro ───────────────────────────────────────────
    val transacaoRepository: com.bioacupunt.financeiro.domain.repository.TransacaoRepository by lazy {
        com.bioacupunt.financeiro.data.repository.TransacaoRepositoryImpl(transacaoDao)
    }

    // ── Repositories ────────────────────────────────────────
    val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl(RetrofitInstance.api, database, syncScheduler)
    }
    val crmPatientRepository: com.bioacupunt.crm.domain.repository.CrmPatientRepository by lazy {
        com.bioacupunt.crm.data.repository.CrmPatientRepositoryImpl(crmPatientDao, cacheManager)
    }

    // ── Agenda ─────────────────────────────────────────────
    val appointmentRepository: com.bioacupunt.agenda.domain.repository.AppointmentRepository by lazy {
        com.bioacupunt.agenda.data.repository.AppointmentRepositoryImpl(appointmentDao)
    }

    // ── Use Cases ──────────────────────────────────────────
    val getPatients: GetPatients by lazy { GetPatients(patientRepository) }
    val createPatient: CreatePatient by lazy { CreatePatient(patientRepository) }

    // ── ViewModel Factories ────────────────────────────────
    val patientsViewModelFactory: PatientsViewModelFactory by lazy {
        PatientsViewModelFactory(getPatients, createPatient, syncScheduler)
    }
    val prontuarioViewModelFactory: ProntuarioViewModelFactory by lazy {
        ProntuarioViewModelFactory(
            cases = com.bioacupunt.prontuario.domain.usecase.ProntuarioUseCases(
                repository = com.bioacupunt.prontuario.data.repository.ProntuarioRepositoryImpl(prontuarioDao)
            )
        )
    }
    val crmViewModelFactory: com.bioacupunt.crm.presentation.CrmViewModelFactory by lazy {
        com.bioacupunt.crm.presentation.CrmViewModelFactory(
            saveCrmPatient = com.bioacupunt.crm.domain.usecase.SaveCrmPatient(crmPatientRepository),
            updateCrmStage = com.bioacupunt.crm.domain.usecase.UpdateCrmStage(crmPatientRepository),
            searchCrmPatients = com.bioacupunt.crm.domain.usecase.SearchCrmPatients(crmPatientRepository)
        )
    }
    val agendaViewModelFactory: com.bioacupunt.agenda.presentation.AgendaViewModelFactory by lazy {
        com.bioacupunt.agenda.presentation.AgendaViewModelFactory(
            getAppointmentsByDate = com.bioacupunt.agenda.domain.usecase.GetAppointmentsByDate(appointmentRepository),
            saveAppointment = com.bioacupunt.agenda.domain.usecase.SaveAppointment(appointmentRepository),
            updateStatus = com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus(appointmentRepository),
            calculateDayStats = com.bioacupunt.agenda.domain.usecase.CalculateDayStats(appointmentRepository)
        )
    }
    val bibliotecaViewModelFactory: com.bioacupunt.biblioteca.presentation.BibliotecaViewModelFactory by lazy {
        com.bioacupunt.biblioteca.presentation.BibliotecaViewModelFactory(
            observe = com.bioacupunt.biblioteca.domain.usecase.ObserveBiblioteca(
                com.bioacupunt.biblioteca.data.repository.BibliotecaRepositoryImpl(bibliotecaDao)
            ),
            search = com.bioacupunt.biblioteca.domain.usecase.SearchBiblioteca(
                com.bioacupunt.biblioteca.data.repository.BibliotecaRepositoryImpl(bibliotecaDao)
            )
        )
    }
    val reportDao: com.bioacupunt.relatorios.data.local.ReportDao by lazy { database.reportDao() }
    val reportRepository: com.bioacupunt.relatorios.domain.repository.ReportRepository by lazy {
        com.bioacupunt.relatorios.data.repository.ReportRepositoryImpl(reportDao)
    }
    val relatoriosUseCases: com.bioacupunt.relatorios.domain.usecase.RelatoriosUseCases by lazy {
        com.bioacupunt.relatorios.domain.usecase.RelatoriosUseCases(reportRepository)
    }
    val relatoriosViewModelFactory: com.bioacupunt.relatorios.presentation.RelatoriosViewModelFactory by lazy {
        com.bioacupunt.relatorios.presentation.RelatoriosViewModelFactory(relatoriosUseCases)
    }

    // ── Seeder ──────────────────────────────────────────────
    fun seedDemoDataIfNeeded() {
        // TODO: Configurar produção - atualmente seed apenas para ambientes dev sem dados.
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val hasPatients = patientDao.count() > 0
            if (hasPatients) return@launch
            val now = java.time.Instant.now().toString()
            val demoPatients = listOf(
                com.bioacupunt.patient.domain.model.Patient(
                    id = 0L, tenantId = 1L, name = "Ana Lima", document = "123", status = "ACTIVE", createdAt = now, updatedAt = now
                ),
                com.bioacupunt.patient.domain.model.Patient(
                    id = 0L, tenantId = 1L, name = "Carlos Souza", document = "456", status = "ACTIVE", createdAt = now, updatedAt = now
                ),
                com.bioacupunt.patient.domain.model.Patient(
                    id = 0L, tenantId = 1L, name = "Maria Santos", document = "789", status = "ACTIVE", createdAt = now, updatedAt = now
                )
            )
            demoPatients.forEach { p ->
                val saved = createPatient(p)
                saved
                val patientId = if (p.id == 0L) 1L else p.id
                crmPatientRepository.save(
                    com.bioacupunt.crm.data.local.CrmPatientEntity(
                        id = patientId,
                        name = p.name,
                        phone = "",
                        email = "",
                        birthDate = "",
                        stage = com.bioacupunt.crm.domain.model.PatientStage.ACTIVE.name,
                        totalSessions = 0,
                        totalRevenueBrl = 0.0,
                        lastVisit = now,
                        nextAppointment = "",
                        tags = "seed",
                        notes = "Seed",
                        referralSource = "",
                        npsScore = null,
                        healthInsurance = "",
                        mainComplaint = "",
                        createdAt = now
                    )
                )
                val apptEntity = com.bioacupunt.agenda.data.local.AppointmentEntity(
                    patientId = patientId,
                    patientName = p.name,
                    date = java.time.LocalDate.now().toString(),
                    time = "08:00",
                    status = com.bioacupunt.agenda.domain.model.AppointmentStatus.SCHEDULED.name,
                    valueBrl = 150.0,
                    paid = true,
                    createdAt = now
                )
                appointmentDao.save(apptEntity)
                transacaoDao.save(
                    com.bioacupunt.financeiro.data.local.TransacaoEntity(
                        patientId = patientId,
                        appointmentId = null,
                        amountBrl = 150.0,
                        date = java.time.LocalDate.now().toString(),
                        type = "PAGAMENTO",
                        status = "PAGO",
                        category = "SESSÃO",
                        createdAt = now
                    )
                )
                reportDao.save(
                    com.bioacupunt.relatorios.data.local.ReportEntity(
                        id = 0,
                        type = "evo",
                        title = "Nota de Evolução — ${p.name}",
                        body = "",
                        filtersJson = "{}",
                        generatedAt = now,
                        patientId = patientId,
                        status = com.bioacupunt.relatorios.domain.model.ReportStatus.DRAFT.name
                    )
                )
            }
        }
    }
}
