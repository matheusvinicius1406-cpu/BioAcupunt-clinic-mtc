package com.bioacupunt.di

import android.content.Context
import com.bioacupunt.auth.data.local.AuthRepositoryImpl
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.cache.AppCacheManager
import com.bioacupunt.core.domain.AppState
import com.bioacupunt.core.domain.AuthState
import com.bioacupunt.core.domain.AIState
import com.bioacupunt.core.domain.NetworkState
import com.bioacupunt.core.domain.SettingsState
import com.bioacupunt.core.domain.SyncState
import com.bioacupunt.core.domain.ThemeState
import com.bioacupunt.core.domain.UserState
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.network.ConnectivityObserver
import com.bioacupunt.core.network.ConnectivityObserverHandler
import com.bioacupunt.core.network.NetworkStatus
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
import com.bioacupunt.security.AuthThrottle
import com.bioacupunt.security.SecurePreferences
import com.bioacupunt.sync.SyncScheduler
import com.bioacupunt.sync.SyncWorkerFactory
import com.bioacupunt.sync.data.local.SyncQueueDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

        // Wire the network layer. This call was missing, which is why the app
        // crashed on launch: RetrofitInstance's interceptors were never configured.
        // Must run *after* _context is set — `tokenManager` and `securePreferences`
        // both resolve through `appContext`.
        RetrofitInstance.init(
            tokenProvider = { tokenManager.getToken() },
            serverUrlProvider = { securePreferences.serverUrl },
        )

        ensureNetworkObserverStarted()
    }

    @Volatile private var networkObserverStarted = false
    private fun ensureNetworkObserverStarted() {
        if (!networkObserverStarted) {
            networkObserverStarted = true
            _seederScope.launch {
                connectivityObserverHandler.status.collect { status ->
                    _appState.value = when (status) {
                        NetworkStatus.ONLINE -> (_appState.value as? AppState.Ready)?.copy(network = NetworkState(isConnected = true))
                        NetworkStatus.OFFLINE -> (_appState.value as? AppState.Ready)?.copy(network = NetworkState(isConnected = false))
                        NetworkStatus.UNKNOWN -> (_appState.value as? AppState.Ready)?.copy(network = NetworkState(isConnected = false))
                    } ?: _appState.value
                }
            }
            connectivityObserverHandler.start()
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
    val authThrottle: AuthThrottle by lazy { AuthThrottle(appContext) }
    /** Gate de login 100% offline (PIN local + biometria). Não fala com backend. */
    val localAuthManager: com.bioacupunt.security.LocalAuthManager by lazy {
        com.bioacupunt.security.LocalAuthManager(securePreferences)
    }
    val tenantManager: TenantManager by lazy { TenantManager(securePreferences) }
    val connectivityObserver: ConnectivityObserver by lazy { ConnectivityObserver(appContext) }
    val connectivityObserverHandler: ConnectivityObserverHandler by lazy { ConnectivityObserverHandler(connectivityObserver) }
    val syncStatusManager: com.bioacupunt.observability.SyncStatusManager by lazy { com.bioacupunt.observability.SyncStatusManager() }

    fun isBiometricAvailable(): Boolean {
        return runCatching {
            val bm = androidx.biometric.BiometricManager.from(appContext)
            bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    // ── Cache ──────────────────────────────────────────────
    val cacheManager: AppCacheManager by lazy { AppCacheManager.getInstance(appContext) }

    // ── Core ───────────────────────────────────────────────
    val appEventManager: com.bioacupunt.core.util.AppEventManager by lazy { com.bioacupunt.core.util.AppEventManager }

    // ── Auth ───────────────────────────────────────────────
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(securePreferences, authThrottle, RetrofitInstance.authApi, tenantManager)
    }
    val tokenManager: com.bioacupunt.auth.data.local.TokenManager by lazy {
        com.bioacupunt.auth.data.local.TokenManager(securePreferences)
    }

    // ── Sync ───────────────────────────────────────────────
    val syncScheduler: SyncScheduler by lazy { SyncScheduler(appContext) }
    val syncWorkerFactory: SyncWorkerFactory by lazy {
        SyncWorkerFactory(engineProvider = { syncEngine }, stateDaoProvider = { syncStateDao })
    }

    /**
     * The writer registry defines what is syncable. Clinical records
     * (prontuário, avaliação MTC, língua/pulso, flags de contraindicação) are
     * deliberately absent — they stay on the device. Adding one here is a
     * decision about sensitive health data leaving the phone, not a wiring
     * detail; see backend/app/api/routers/sync.py.
     */
    val syncEngine: com.bioacupunt.sync.SyncEngine by lazy {
        com.bioacupunt.sync.SyncEngine(
            api = RetrofitInstance.syncApi,
            stateDao = syncStateDao,
            conflictDao = syncConflictDao,
            writers = mapOf(
                "patient" to com.bioacupunt.sync.CrmPatientSyncWriter(
                    dao = crmPatientDao,
                    tenantId = { tenantManager.requireTenantId() },
                ),
                "appointment" to com.bioacupunt.sync.AppointmentSyncWriter(
                    dao = appointmentDao,
                    patientDao = crmPatientDao,
                    tenantId = { tenantManager.requireTenantId() },
                ),
                "transaction" to com.bioacupunt.sync.TransacaoSyncWriter(
                    dao = transacaoDao,
                    patientDao = crmPatientDao,
                ),
            ),
        )
    }

    // ── Database ───────────────────────────────────────────
    val database: AppDatabase by lazy { DatabaseModule.provideAppDatabase(appContext) }

    // ── Backup / Google Drive ──────────────────────────────
    val googleDriveClient: com.bioacupunt.backup.GoogleDriveClient by lazy {
        com.bioacupunt.backup.GoogleDriveClient(appContext)
    }
    val backupManager: com.bioacupunt.backup.BackupManager by lazy {
        com.bioacupunt.backup.BackupManager(appContext, database)
    }

    // ── DAOs ───────────────────────────────────────────────
    val patientDao: PatientDao by lazy { database.patientDao() }
    val syncQueueDao: SyncQueueDao by lazy { database.syncQueueDao() }
    val syncStateDao: com.bioacupunt.sync.data.local.SyncStateDao by lazy { database.syncStateDao() }
    val syncConflictDao: com.bioacupunt.sync.data.local.SyncConflictDao by lazy { database.syncConflictDao() }
    val knowledgeNodeDao: KnowledgeNodeDao by lazy { database.knowledgeNodeDao() }
    val crmPatientDao: com.bioacupunt.crm.data.local.CrmPatientDao by lazy { database.crmPatientDao() }
    val appointmentDao: com.bioacupunt.agenda.data.local.AppointmentDao by lazy { database.appointmentDao() }
    val transacaoDao: com.bioacupunt.financeiro.data.local.TransacaoDao by lazy { database.transacaoDao() }
    val prontuarioDao: com.bioacupunt.prontuario.data.local.ProntuarioDao by lazy { database.prontuarioDao() }

    // --- Prontuário Supremo (structured TCM chart + clinical safety) ---
    val mtcAssessmentDao: com.bioacupunt.prontuario.data.local.MtcAssessmentDao by lazy {
        database.mtcAssessmentDao()
    }

    // --- Prontuário: exames (vitals/labs/medications/allergies) + documentos ---
    val exameDao: com.bioacupunt.prontuario.data.local.ExameDao by lazy { database.exameDao() }
    val prontuarioDocumentDao: com.bioacupunt.prontuario.data.local.ProntuarioDocumentDao by lazy {
        database.prontuarioDocumentDao()
    }

    /**
     * Single shared instance. The rule set is a clinic-wide policy, not per-screen
     * state — every caller must screen against exactly the same rules.
     */
    val clinicalSafetyEngine: com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine by lazy {
        com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine()
    }

    val mtcAssessmentRepository: com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository by lazy {
        com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository(
            dao = mtcAssessmentDao,
            safetyEngine = clinicalSafetyEngine,
        )
    }

    fun supremoViewModelFactory(patientId: Long) =
        com.bioacupunt.prontuario.presentation.SupremoViewModelFactory(
            repository = mtcAssessmentRepository,
            patientId = patientId,
        )

    val exameRepository: com.bioacupunt.prontuario.domain.repository.ExameRepository by lazy {
        com.bioacupunt.prontuario.data.repository.ExameRepositoryImpl(exameDao)
    }
    val exameUseCases: com.bioacupunt.prontuario.domain.usecase.ExameUseCases by lazy {
        com.bioacupunt.prontuario.domain.usecase.ExameUseCases(exameRepository)
    }
    val prontuarioDocumentRepository: com.bioacupunt.prontuario.domain.repository.ProntuarioDocumentRepository by lazy {
        com.bioacupunt.prontuario.data.repository.ProntuarioDocumentRepositoryImpl(prontuarioDocumentDao)
    }
    val prontuarioDocumentUseCases: com.bioacupunt.prontuario.domain.usecase.ProntuarioDocumentUseCases by lazy {
        com.bioacupunt.prontuario.domain.usecase.ProntuarioDocumentUseCases(prontuarioDocumentRepository)
    }
    fun evolucaoViewModelFactory(patientId: Long) =
        com.bioacupunt.prontuario.presentation.EvolucaoViewModelFactory(
            mtcAssessmentRepository = mtcAssessmentRepository,
            observeEntries = com.bioacupunt.prontuario.domain.usecase.ObserveEntries(
                com.bioacupunt.prontuario.data.repository.ProntuarioRepositoryImpl(prontuarioDao)
            ),
            patientId = patientId,
        )

    fun exameViewModelFactory(patientId: Long) =
        com.bioacupunt.prontuario.presentation.ExameViewModelFactory(
            exameUseCases = exameUseCases,
            documentUseCases = prontuarioDocumentUseCases,
            patientId = patientId,
        )

    val bibliotecaDao: com.bioacupunt.biblioteca.data.local.BibliotecaDao by lazy { database.bibliotecaDao() }
    val favoriteArticleDao: com.bioacupunt.biblioteca.data.local.FavoriteArticleDao by lazy { database.favoriteArticleDao() }

    // ── Biblioteca: pipeline de ingestão + curadoria ───────
    val libraryStagingRepository: com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository by lazy {
        com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository(bibliotecaDao)
    }
    val libraryReviewViewModelFactory: com.bioacupunt.biblioteca.presentation.LibraryReviewViewModelFactory by lazy {
        com.bioacupunt.biblioteca.presentation.LibraryReviewViewModelFactory(libraryStagingRepository)
    }

    // ── Financeiro ─────────────────────────────────────────
    val transacaoRepository: com.bioacupunt.financeiro.domain.repository.TransacaoRepository by lazy {
        com.bioacupunt.financeiro.data.repository.TransacaoRepositoryImpl(transacaoDao)
    }

    // ── Repositories ───────────────────────────────────────
    val patientRepository: PatientRepository by lazy {
        PatientRepositoryImpl(RetrofitInstance.api, database, syncScheduler)
    }
    val crmPatientRepository: com.bioacupunt.crm.domain.repository.CrmPatientRepository by lazy {
        com.bioacupunt.crm.data.repository.CrmPatientRepositoryImpl(crmPatientDao, cacheManager, tenantManager)
    }

    // ── Agenda ─────────────────────────────────────────────
    val appointmentRepository: com.bioacupunt.agenda.domain.repository.AppointmentRepository by lazy {
        com.bioacupunt.agenda.data.repository.AppointmentRepositoryImpl(appointmentDao, tenantManager)
    }

    // ── Use Cases ──────────────────────────────────────────
    val getPatients: GetPatients by lazy { GetPatients(patientRepository) }
    val createPatient: CreatePatient by lazy { CreatePatient(patientRepository) }

    // ── ViewModel Factories ────────────────────────────────
    val patientsViewModelFactory: PatientsViewModelFactory by lazy {
        PatientsViewModelFactory(getPatients, createPatient, syncScheduler)
    }
    val prontuarioViewModelFactory: com.bioacupunt.prontuario.presentation.ProntuarioViewModelFactory by lazy {
        com.bioacupunt.prontuario.presentation.ProntuarioViewModelFactory(
            cases = com.bioacupunt.prontuario.domain.usecase.ProntuarioUseCases(
                repository = com.bioacupunt.prontuario.data.repository.ProntuarioRepositoryImpl(prontuarioDao)
            )
        )
    }
    val crmViewModelFactory: com.bioacupunt.crm.presentation.CrmViewModelFactory by lazy {
        com.bioacupunt.crm.presentation.CrmViewModelFactory(
            saveCrmPatient = com.bioacupunt.crm.domain.usecase.SaveCrmPatient(crmPatientRepository),
            updateCrmStage = com.bioacupunt.crm.domain.usecase.UpdateCrmStage(crmPatientRepository),
            getCrmPatients = com.bioacupunt.crm.domain.usecase.GetCrmPatients(crmPatientRepository),
            repository = crmPatientRepository,
            tenantManager = tenantManager
        )
    }
    val conflictViewModelFactory: com.bioacupunt.sync.presentation.ConflictViewModelFactory by lazy {
        com.bioacupunt.sync.presentation.ConflictViewModelFactory(
            conflictDao = syncConflictDao,
            engine = syncEngine,
        )
    }
    val dashboardViewModelFactory: com.bioacupunt.dashboard.presentation.DashboardViewModelFactory by lazy {
        com.bioacupunt.dashboard.presentation.DashboardViewModelFactory(
            authRepository = authRepository,
            appointmentRepository = appointmentRepository,
            crmPatientRepository = crmPatientRepository,
            transacaoRepository = transacaoRepository,
        )
    }
    val agendaViewModelFactory: com.bioacupunt.agenda.presentation.AgendaViewModelFactory by lazy {
        com.bioacupunt.agenda.presentation.AgendaViewModelFactory(
            getAppointmentsByDate = com.bioacupunt.agenda.domain.usecase.GetAppointmentsByDate(appointmentRepository),
            getAppointmentsInRange = com.bioacupunt.agenda.domain.usecase.GetAppointmentsInRange(appointmentRepository),
            saveAppointment = com.bioacupunt.agenda.domain.usecase.SaveAppointment(appointmentRepository),
            updateStatus = com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus(appointmentRepository),
            calculateDayStats = com.bioacupunt.agenda.domain.usecase.CalculateDayStats(appointmentRepository),
            crmPatientRepository = crmPatientRepository
        )
    }
    fun atendimentoViewModelFactory(appointmentId: Long) =
        com.bioacupunt.agenda.presentation.AtendimentoViewModelFactory(
            appointmentRepository = appointmentRepository,
            updateAppointmentStatus = com.bioacupunt.agenda.domain.usecase.UpdateAppointmentStatus(appointmentRepository),
            addEntry = com.bioacupunt.prontuario.domain.usecase.AddEntry(
                com.bioacupunt.prontuario.data.repository.ProntuarioRepositoryImpl(prontuarioDao)
            ),
            appointmentId = appointmentId,
        )
    val bibliotecaViewModelFactory: com.bioacupunt.biblioteca.presentation.BibliotecaViewModelFactory by lazy {
        com.bioacupunt.biblioteca.presentation.BibliotecaViewModelFactory(
            askLibrary = askLibrary,
            toggleFavoriteArticle = com.bioacupunt.biblioteca.domain.usecase.ToggleFavoriteArticle(favoriteArticleDao),
            observeFavorites = favoriteArticleDao.observeAll().map { list -> list.map { fav -> fav.articleId }.toSet() },
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
    val financeiroViewModelFactory: com.bioacupunt.financeiro.presentation.FinanceiroViewModelFactory by lazy {
        com.bioacupunt.financeiro.presentation.FinanceiroViewModelFactory(
            com.bioacupunt.financeiro.domain.usecase.ObserveTransactions(transacaoRepository)
        )
    }

    // ── AI ─────────────────────────────────────────────────
    val localModelManager: com.bioacupunt.ai.data.provider.LocalModelManager by lazy {
        com.bioacupunt.ai.data.provider.LocalModelManager(appContext)
    }

    val localLlmProvider: com.bioacupunt.ai.data.provider.LocalLlmProvider by lazy {
        com.bioacupunt.ai.data.provider.LocalLlmProvider(appContext, localModelManager)
    }

    private val aiOrchestrator: com.bioacupunt.ai.orchestrator.AiOrchestrator by lazy {
        com.bioacupunt.ai.orchestrator.ScoredAiOrchestrator(
            providers = com.bioacupunt.ai.registry.SimpleProviderRegistry().also { registry ->
                kotlinx.coroutines.runBlocking {
                    // IA 100% local: o único provider é o Gemma no dispositivo. Sem
                    // nuvem, sem Gemini — dado clínico nunca sai do aparelho. Reporta
                    // isAvailable() == false até o modelo ser baixado; nesse meio-tempo o
                    // orquestrador devolve "IA não configurada" (degrada, não quebra).
                    registry.register(localLlmProvider)
                    // MockProvider is deliberately NOT registered.
                    //
                    // It answers every prompt with "Mock resposta para: <prompt>"
                    // and reports isAvailable() == true unconditionally, so once
                    // the orchestrator started honouring availability it became
                    // the *selected* provider on any device without a downloaded
                    // model and without a Gemini key — which is every device
                    // today. The doctor would have been shown placeholder text
                    // in the clinical assistant.
                    //
                    // With no provider available the orchestrator now returns
                    // NoProviderAvailable and the UI says the assistant is not
                    // configured. "I cannot answer" is a safe answer; a fake one
                    // dressed as an answer is not. Tests construct MockProvider
                    // directly — it does not need to be in the app's graph.
                }
            },
            healthRegistry = com.bioacupunt.ai.health.DefaultHealthRegistry()
        )
    }
    val aiRepository: com.bioacupunt.ai.core.AiRepository by lazy {
        com.bioacupunt.ai.data.repository.AiRepositoryImpl(aiOrchestrator)
    }
    // ── Biblioteca: busca BM25 + RAG ancorado ──────────────
    val mtcRetriever: com.bioacupunt.biblioteca.domain.search.MtcRetriever by lazy {
        com.bioacupunt.biblioteca.domain.search.MtcRetriever(
            com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles,
        )
    }

    /**
     * The only sanctioned path for asking the AI a knowledge question: it refuses to
     * call the model when the library has no evidence. See AskLibraryUseCase.
     */
    val askLibrary: com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase by lazy {
        com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase(mtcRetriever, aiRepository)
    }

    val aiAssistantViewModelFactory: com.bioacupunt.biblioteca.presentation.AiAssistantViewModelFactory by lazy {
        com.bioacupunt.biblioteca.presentation.AiAssistantViewModelFactory(askLibrary)
    }

    val aiHealthRegistry: com.bioacupunt.ai.health.HealthRegistry by lazy {
        com.bioacupunt.ai.health.DefaultHealthRegistry()
    }
    val generateAiResponse: com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase by lazy {
        com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase(aiRepository)
    }
    val aiConfigManager: com.bioacupunt.ai.config.AiConfigManager by lazy {
        com.bioacupunt.ai.config.AndroidAiConfigManager(appContext)
    }
    val aiSecretsProvider: com.bioacupunt.ai.config.AiSecretsProvider by lazy {
        com.bioacupunt.ai.config.AndroidAiSecretsProvider(appContext)
    }

    // ── Seeder ──────────────────────────────────────────────
    private val _seederScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun seedDemoDataIfNeeded() {
        // TODO: Configurar produção - atualmente seed apenas para ambientes dev sem dados.
        if (!com.bioacupunt.security.AppHardening.isDebugDebuggable(appContext)) return
        // Fire-and-forget on a SupervisorJob scope: without this guard an
        // exception here (a failed insert, a constraint violation) would reach
        // the default uncaught-exception handler and crash the whole app on
        // startup instead of just skipping the demo data.
        _seederScope.launch {
            runCatching { seedDemoData() }
                .onFailure { e -> com.bioacupunt.observability.AppLogger.e("AppContainer", "Demo seed failed", e) }
        }
    }

    /**
     * Seeds demo data, CRM-first.
     *
     * `crm_patients` is the patient registry — every clinical, financial and
     * scheduling row references it — so the CRM row must be created first and
     * its *generated* id used for everything that follows.
     *
     * The previous version wrote the legacy `patients` row first and then
     * assumed the id: `if (p.id == 0L) 1L else p.id`. Since every demo patient
     * was declared with `id = 0L`, all three were written to CRM id 1, each
     * overwriting the last (the DAO uses REPLACE). Three patients went in and
     * one came out, wearing the last one's name.
     */
    private suspend fun seedDemoData() {
            if (crmPatientDao.count(tenantManager.requireTenantId()) > 0) return
            val now = java.time.Instant.now().toString()
            val demoPatients = listOf(
                "Ana Lima" to "123",
                "Carlos Souza" to "456",
                "Maria Santos" to "789",
            )
            demoPatients.forEach { (patientName, document) ->
                val savedCrm = crmPatientRepository.save(
                    com.bioacupunt.crm.domain.model.CrmPatient(
                        id = 0L,
                        tenantId = 1L,
                        name = patientName,
                        phone = "",
                        email = "",
                        birthDate = "",
                        stage = com.bioacupunt.crm.domain.model.PatientStage.ACTIVE.name,
                        totalSessions = 0,
                        totalRevenueBrl = 0.0,
                        lastVisit = now,
                        nextAppointment = "",
                        tags = listOf("seed"),
                        notes = "Seed",
                        referralSource = "",
                        npsScore = null,
                        healthInsurance = "",
                        mainComplaint = "",
                        createdAt = now
                    )
                )
                val patientId = (savedCrm as? com.bioacupunt.core.util.Result.Success)?.data?.id
                    ?: return@forEach
                createPatient(
                    com.bioacupunt.patient.domain.model.Patient(
                        id = 0L, tenantId = 1L, name = patientName, document = document,
                        status = "ACTIVE", createdAt = now, updatedAt = now
                    )
                )
                val p = com.bioacupunt.patient.domain.model.Patient(
                    id = patientId, tenantId = 1L, name = patientName, document = document,
                    status = "ACTIVE", createdAt = now, updatedAt = now
                )
                val apptEntity = com.bioacupunt.agenda.data.local.AppointmentEntity(
                    tenantId = 1L,
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
