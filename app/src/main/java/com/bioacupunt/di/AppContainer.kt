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
import com.bioacupunt.core.multitenancy.TenantManagerImpl
import com.bioacupunt.core.network.ConnectivityObserver
import com.bioacupunt.core.network.ConnectivityObserverHandler
import com.bioacupunt.core.network.NetworkStatus
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.data.repository.LegacyKnowledgeNodeRepository
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.patient.domain.usecase.CreatePatient
import com.bioacupunt.patient.domain.usecase.GetPatients
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
import kotlinx.coroutines.withContext
import com.bioacupunt.pharma.domain.ingestion.toDomain

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
    val tenantManager: TenantManager by lazy { TenantManagerImpl(securePreferences) }
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
            status = syncStatusManager,
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
                    tenantId = { tenantManager.requireTenantId() },
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
    val knowledgeCoreDao: com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao by lazy { database.knowledgeCoreDao() }
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

    /**
     * Extração puramente extrativa do Motivo da Consulta (nunca diagnóstico/sugestão
     * de conduta) — ver `StructureChiefComplaintUseCase`. Reaproveita o mesmo
     * `aiRepository` do resto do app; local ou nuvem conforme a médica configurou em
     * Ajustes > IA, mesma engrenagem de sempre.
     */
    val structureChiefComplaintUseCase: com.bioacupunt.prontuario.domain.usecase.StructureChiefComplaintUseCase by lazy {
        com.bioacupunt.prontuario.domain.usecase.StructureChiefComplaintUseCase(aiRepository)
    }

    /**
     * SÍNTESE CLÍNICA — IA analisa TODO o prontuário e sugere diagnóstico + plano.
     * R1/R2/R4 permanecem intactos: este é um caminho SEPARADO que não substitui
     * o ClinicalSafetyEngine (R1), não altera o gate do AskLibraryUseCase (R2), e
     * não gera conteúdo para a biblioteca (R4).
     *
     * A médica REVISA cada componente e decide o que aceitar — NUNCA salva
     * automaticamente.
     */
    val clinicalSynthesisUseCase: com.bioacupunt.prontuario.domain.usecase.ClinicalSynthesisUseCase by lazy {
        com.bioacupunt.prontuario.domain.usecase.ClinicalSynthesisUseCase(
            ai = aiRepository,
            mtcRetriever = mtcRetriever,
        )
    }

    fun supremoViewModelFactory(patientId: Long) =
        com.bioacupunt.prontuario.presentation.SupremoViewModelFactory(
            repository = mtcAssessmentRepository,
            patientId = patientId,
            structureChiefComplaint = structureChiefComplaintUseCase,
            clinicalSynthesisUseCase = clinicalSynthesisUseCase,
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
    val articleSearchDao: com.bioacupunt.biblioteca.data.local.dao.ArticleSearchDao by lazy { database.articleSearchDao() }

    // ── Knowledge Core (canonical knowledge boundary) ─────────────────
    val knowledgeCoreRepository: com.bioacupunt.mtc.knowledge.repository.KnowledgeRepository by lazy {
        com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeRepository(knowledgeCoreDao)
    }
    val knowledgeCoreImporter: com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter by lazy {
        com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter(knowledgeCoreDao)
    }
    val libraryAdapter: com.bioacupunt.mtc.knowledge.data.LibraryAdapter by lazy {
        com.bioacupunt.mtc.knowledge.data.LibraryAdapter()
    }
    val mkisAdapter: com.bioacupunt.mtc.knowledge.data.MkisAdapter by lazy {
        com.bioacupunt.mtc.knowledge.data.MkisAdapter()
    }
    val knowledgeCoreFtsSyncer: com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsSyncer by lazy {
        com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsSyncer { database.openHelper.writableDatabase }
    }
    val knowledgeCoreFtsDao: com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsDao by lazy {
        com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsDao { database.openHelper.writableDatabase }
    }
    val knowledgeSearchRepository: com.bioacupunt.mtc.knowledge.repository.KnowledgeSearchRepository by lazy {
        com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeSearchRepository(
            knowledgeCoreDao,
        ) { database.openHelper.writableDatabase }
    }
    val knowledgeCoverageAudit: com.bioacupunt.mtc.knowledge.data.KnowledgeCoverageAudit by lazy {
        com.bioacupunt.mtc.knowledge.data.KnowledgeCoverageAudit(
            bibliotecaDao,
            knowledgeNodeDao,
            knowledgeCoreDao,
            libraryAdapter,
            mkisAdapter,
        )
    }
    val legacyImporter: com.bioacupunt.mtc.knowledge.data.LegacyImporter by lazy {
        com.bioacupunt.mtc.knowledge.data.LegacyImporter(
            bibliotecaDao,
            knowledgeNodeDao,
            knowledgeCoreImporter,
            libraryAdapter,
            mkisAdapter,
        )
    }

    // ── Biblioteca: pipeline de ingestão + curadoria ───────
    val libraryStagingRepository: com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository by lazy {
        com.bioacupunt.biblioteca.data.repository.LibraryStagingRepository(bibliotecaDao)
    }

    // ── Biblioteca: tradutor automático (roda após aprovação na Curadoria) ──
    val articleTranslationDao: com.bioacupunt.biblioteca.data.local.ArticleTranslationDao by lazy {
        database.articleTranslationDao()
    }
    val articleTranslationRepository: com.bioacupunt.biblioteca.data.repository.ArticleTranslationRepository by lazy {
        com.bioacupunt.biblioteca.data.repository.ArticleTranslationRepository(articleTranslationDao)
    }
    val translateArticleUseCase: com.bioacupunt.biblioteca.domain.usecase.TranslateArticleUseCase by lazy {
        com.bioacupunt.biblioteca.domain.usecase.TranslateArticleUseCase(aiRepository)
    }
    fun translationTargetLanguage(): com.bioacupunt.biblioteca.domain.model.TranslationLanguage =
        com.bioacupunt.biblioteca.domain.model.TranslationLanguage.byCode(securePreferences.translationTargetLanguage)
            ?: com.bioacupunt.biblioteca.domain.model.TranslationLanguage.default

    val libraryReviewViewModelFactory: com.bioacupunt.biblioteca.presentation.LibraryReviewViewModelFactory by lazy {
        com.bioacupunt.biblioteca.presentation.LibraryReviewViewModelFactory(
            repo = libraryStagingRepository,
            onContentChanged = { ftsSearchService.notifyContentChanged() },
            generateStudyMaterial = generateStudyMaterialUseCase,
            flashcardRepository = flashcardRepository,
            simulatedCaseRepository = simulatedCaseRepository,
            onArticleApproved = { article ->
                _seederScope.launch {
                    com.bioacupunt.biblioteca.data.worker.ArticleTranslationWorker.enqueue(
                        appContext, article.id, translationTargetLanguage(),
                    )
                }
            },
        )
    }

    // ── Educação: Flashcards ────────────────────────────────
    val flashcardDao: com.bioacupunt.educacao.data.local.FlashcardDao by lazy { database.flashcardDao() }
    val flashcardRepository: com.bioacupunt.educacao.domain.repository.FlashcardRepository by lazy {
        com.bioacupunt.educacao.data.repository.FlashcardRepositoryImpl(
            dao = flashcardDao,
            tenantId = { tenantManager.requireTenantId() },
        )
    }
    val flashcardsViewModelFactory: com.bioacupunt.educacao.presentation.FlashcardsViewModelFactory by lazy {
        com.bioacupunt.educacao.presentation.FlashcardsViewModelFactory(
            repository = flashcardRepository,
            // União baseline+aprovados, mesmo padrão do FtsSearchService (R4: só o que
            // já passou por revisão humana entra como fonte de flashcard).
            sourceArticles = {
                com.bioacupunt.biblioteca.data.MtcKnowledgeBase.articles + libraryStagingRepository.approvedArticles()
            },
        )
    }

    // ── Educação: Simulador de Casos Clínicos ────────────────
    val simulatedCaseDao: com.bioacupunt.educacao.data.local.SimulatedCaseDao by lazy { database.simulatedCaseDao() }
    val simulatedCaseRepository: com.bioacupunt.educacao.domain.repository.SimulatedCaseRepository by lazy {
        com.bioacupunt.educacao.data.repository.SimulatedCaseRepositoryImpl(
            dao = simulatedCaseDao,
            tenantId = { tenantManager.requireTenantId() },
        )
    }
    val simuladorViewModelFactory: com.bioacupunt.educacao.presentation.SimuladorViewModelFactory by lazy {
        com.bioacupunt.educacao.presentation.SimuladorViewModelFactory(repository = simulatedCaseRepository)
    }

    /**
     * Gera rascunho de flashcards + caso simulado a partir de UM artigo já aprovado —
     * chamado pela Curadoria logo após aprovar (R4: rascunho, nunca salvo automático;
     * ver [com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase]).
     */
    val generateStudyMaterialUseCase: com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase by lazy {
        com.bioacupunt.educacao.domain.usecase.GenerateStudyMaterialUseCase(aiRepository)
    }

    // ── Farmacologia: Pharma Library + Smart Prescription ──
    val medicamentoDao: com.bioacupunt.pharma.data.local.MedicamentoDao by lazy { database.medicamentoDao() }
    val medicamentoFtsDao: com.bioacupunt.pharma.data.local.MedicamentoFtsDao by lazy { database.medicamentoFtsDao() }
    val formularioMedicamentoDao: com.bioacupunt.pharma.data.local.FormularioMedicamentoDao by lazy { database.formularioMedicamentoDao() }
    val prescricaoDao: com.bioacupunt.pharma.data.local.PrescricaoDao by lazy { database.prescricaoDao() }

    val medicamentoRepository: com.bioacupunt.pharma.domain.repository.MedicamentoRepository by lazy {
        com.bioacupunt.pharma.data.repository.MedicamentoRepositoryImpl(medicamentoDao, medicamentoFtsDao)
    }
    val formularioMedicamentoRepository: com.bioacupunt.pharma.domain.repository.FormularioMedicamentoRepository by lazy {
        com.bioacupunt.pharma.data.repository.FormularioMedicamentoRepositoryImpl(formularioMedicamentoDao)
    }
    val prescricaoRepository: com.bioacupunt.pharma.domain.repository.PrescricaoRepository by lazy {
        com.bioacupunt.pharma.data.repository.PrescricaoRepositoryImpl(prescricaoDao, tenantManager)
    }
    val pharmaSafetyEngine: com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine by lazy {
        com.bioacupunt.pharma.domain.safety.PharmaSafetyEngine()
    }

    /** Mesma resolução de identidade usada pelo override do veto clínico em ProntuarioScreen. */
    private fun currentUserId(): String = runCatching {
        authRepository.getCurrentUser()?.id?.toString() ?: securePreferences.pinHash.take(8)
    }.getOrDefault("unknown").ifBlank { "unknown" }

    val farmacologiaViewModelFactory: com.bioacupunt.pharma.presentation.FarmacologiaViewModelFactory by lazy {
        com.bioacupunt.pharma.presentation.FarmacologiaViewModelFactory(
            medicamentoRepository = medicamentoRepository,
            formularioMedicamentoRepository = formularioMedicamentoRepository,
            tenantManager = tenantManager,
        )
    }

    val farmacologiaCuradoriaViewModelFactory: com.bioacupunt.pharma.presentation.FarmacologiaCuradoriaViewModelFactory by lazy {
        com.bioacupunt.pharma.presentation.FarmacologiaCuradoriaViewModelFactory(
            medicamentoRepository = medicamentoRepository,
            formularioMedicamentoRepository = formularioMedicamentoRepository,
            tenantManager = tenantManager,
            currentUser = ::currentUserId,
        )
    }

    fun prescricaoViewModelFactory(patientId: Long) =
        com.bioacupunt.pharma.presentation.PrescricaoViewModelFactory(
            medicamentoRepository = medicamentoRepository,
            formularioMedicamentoRepository = formularioMedicamentoRepository,
            prescricaoRepository = prescricaoRepository,
            exameRepository = exameRepository,
            mtcAssessmentRepository = mtcAssessmentRepository,
            safetyEngine = pharmaSafetyEngine,
            tenantManager = tenantManager,
            patientId = patientId,
            currentUser = ::currentUserId,
        )

    /**
     * Popula o catálogo ANVISA a partir dos packs em assets, uma única vez — ao
     * contrário de [seedDemoDataIfNeeded], roda em TODO build (inclusive release):
     * não é dado de demonstração, é o catálogo real que toda instalação precisa pra a
     * tela Farmacologia funcionar. Fire-and-forget na mesma [_seederScope] — não pode
     * bloquear o primeiro frame inserindo milhares de linhas.
     */
    fun seedPharmaCatalogIfNeeded() {
        _seederScope.launch {
            runCatching {
                if (medicamentoRepository.count() > 0) return@runCatching
                val items = com.bioacupunt.pharma.data.packs.AnvisaCatalogPacks.load(appContext)
                    .flatMap { pack -> pack.items }
                    .map { item -> item.toDomain() }
                medicamentoRepository.seedIfEmpty(items)
            }.onFailure { e ->
                com.bioacupunt.observability.AppLogger.e("AppContainer", "Pharma catalog seed failed", e)
            }
        }
    }

    // ── Financeiro ─────────────────────────────────────────
    val transacaoRepository: com.bioacupunt.financeiro.domain.repository.TransacaoRepository by lazy {
        com.bioacupunt.financeiro.data.repository.TransacaoRepositoryImpl(transacaoDao, tenantManager)
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

    // ── Lembretes de consulta (AlarmManager real) ──────────
    val appointmentReminderScheduler: com.bioacupunt.agenda.AppointmentReminderScheduler by lazy {
        com.bioacupunt.agenda.AppointmentReminderScheduler(appContext, appointmentDao, securePreferences)
    }

    /** Reagenda os alarmes de consulta (boot, volta ao primeiro plano, mudança de preferência). */
    fun rescheduleAppointmentReminders() {
        val tenant = runCatching { tenantManager.currentTenantId() }.getOrNull() ?: return
        _seederScope.launch {
            runCatching { appointmentReminderScheduler.rescheduleAll(tenant) }
                .onFailure { com.bioacupunt.observability.AppLogger.e("AppContainer", "rescheduleAppointmentReminders falhou", it) }
        }
    }

    // ── Use Cases ──────────────────────────────────────────
    val getPatients: GetPatients by lazy { GetPatients(patientRepository) }
    val createPatient: CreatePatient by lazy { CreatePatient(patientRepository) }

    // ── ViewModel Factories ────────────────────────────────
    // (patientsViewModelFactory removido: a PatientsScreen que o consumia era órfã,
    //  substituída pela CrmScreen. GetPatients/CreatePatient seguem no container —
    //  createPatient ainda é usado no seeding.)
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
            tenantManager = tenantManager,
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
            hybridSearchService = hybridSearchService,
            knowledgeNodeDao = database.knowledgeNodeDao(),
            observeFavorites = favoriteArticleDao.observeAll().map { list -> list.map { fav -> fav.articleId }.toSet() },
            observeApprovedArticles = libraryStagingRepository.observeApprovedArticles(),
        )
    }
    val reportDao: com.bioacupunt.relatorios.data.local.ReportDao by lazy { database.reportDao() }
    val reportRepository: com.bioacupunt.relatorios.domain.repository.ReportRepository by lazy {
        com.bioacupunt.relatorios.data.repository.ReportRepositoryImpl(reportDao)
    }
    val relatoriosUseCases: com.bioacupunt.relatorios.domain.usecase.RelatoriosUseCases by lazy {
        com.bioacupunt.relatorios.domain.usecase.RelatoriosUseCases(reportRepository)
    }
    /**
     * Geração REAL de relatórios a partir do prontuário do paciente (nunca IA
     * inventando conteúdo clínico — ver o use case). O nome digitado no diálogo
     * resolve contra o CRM; sem paciente correspondente, erro honesto, nunca
     * relatório vazio.
     */
    val generateReportUseCase: com.bioacupunt.relatorios.domain.usecase.GenerateReportUseCase by lazy {
        com.bioacupunt.relatorios.domain.usecase.GenerateReportUseCase(
            crmPatientRepository = crmPatientRepository,
            mtcAssessmentRepository = mtcAssessmentRepository,
            appointmentRepository = appointmentRepository,
            clinicName = { runCatching { securePreferences.clinicName }.getOrDefault("").ifBlank { "Clínica BioAcupunt" } },
            professionalName = { runCatching { securePreferences.professionalName }.getOrDefault("").ifBlank { "Dra. Camila" } },
            tcleText = { runCatching { securePreferences.tcleText }.getOrDefault("") },
        )
    }
    val relatoriosViewModelFactory: com.bioacupunt.relatorios.presentation.RelatoriosViewModelFactory by lazy {
        com.bioacupunt.relatorios.presentation.RelatoriosViewModelFactory(relatoriosUseCases, generateReportUseCase)
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

    // ── Embedding (MKIS On-Device) ────────────────────────
    val embeddingService: com.bioacupunt.ai.embedding.EmbeddingService by lazy {
        com.bioacupunt.ai.embedding.EmbeddingService(appContext)
    }

    val vecKnowledgeNodeRepository: com.bioacupunt.data.local.database.VecKnowledgeNodeRepository by lazy {
        com.bioacupunt.data.local.database.VecKnowledgeNodeRepository.from(database)
    }

    // ── Content Extractor (MKIS Pipeline) ─────────────────
    val contentExtractor: com.bioacupunt.mkis.domain.pipeline.ContentExtractor by lazy {
        com.bioacupunt.mkis.domain.pipeline.ContentExtractor(aiRepository)
    }

    // ── Pipeline Service (MKIS On-Device) ─────────────────
    val pipelineService: com.bioacupunt.mkis.domain.pipeline.PipelineService by lazy {
        com.bioacupunt.mkis.domain.pipeline.PipelineService(
            ingestionJobDao = database.ingestionJobDao(),
            knowledgeNodeDao = database.knowledgeNodeDao(),
            vecRepo = vecKnowledgeNodeRepository,
            embeddingService = embeddingService,
            contentExtractor = contentExtractor,
        )
    }

    // ── Pipeline Monitor (MKIS UI) ────────────────────────
    fun pipelineMonitorViewModelFactory(): com.bioacupunt.ui.screens.PipelineMonitorViewModelFactory {
        return com.bioacupunt.ui.screens.PipelineMonitorViewModelFactory(
            ingestionJobDao = database.ingestionJobDao(),
            pipelineService = pipelineService,
        )
    }

    val localLlmProvider: com.bioacupunt.ai.data.provider.LocalLlmProvider by lazy {
        com.bioacupunt.ai.data.provider.LocalLlmProvider(appContext, localModelManager)
    }

    /**
     * 100% on-device, sem nuvem. O provider de nuvem (Gemini) foi removido de propósito
     * (2026-07-29, pedido explícito da médica) — o app inteiro roda sobre o modelo local
     * (Phi-4 Mini Instruct) ou não responde. Sem provider disponível o orquestrador
     * devolve NoProviderAvailable e a UI diz que o assistente ainda não está pronto
     * (baixe o modelo em Ajustes > IA). "Não sei responder" é uma resposta segura; uma
     * resposta falsa vestida de resposta não é. Não existe mais um provider de mentira
     * aqui para registrar por acidente: MockProvider/FakeProvider foram deletados antes
     * disso, e não há chave de API nem dado clínico que possa vazar para fora do aparelho.
     */
    private val aiOrchestrator: com.bioacupunt.ai.orchestrator.AiOrchestrator by lazy {
        com.bioacupunt.ai.orchestrator.ScoredAiOrchestrator(
            providers = com.bioacupunt.ai.registry.SimpleProviderRegistry().also { registry ->
                kotlinx.coroutines.runBlocking { registry.register(localLlmProvider) }
            },
            healthRegistry = com.bioacupunt.ai.health.DefaultHealthRegistry(),
        )
    }
    val aiRepository: com.bioacupunt.ai.core.AiRepository by lazy {
        com.bioacupunt.ai.data.repository.AiRepositoryImpl(aiOrchestrator)
    }
    // ── Biblioteca: índice FTS4 + busca ──────────────────
    val ftsSearchService: com.bioacupunt.biblioteca.data.search.FtsSearchService by lazy {
        com.bioacupunt.biblioteca.data.search.FtsSearchService(
            articleSearchDao,
            libraryStagingRepository,
        )
    }

    // ── Busca Híbrida: FTS5 + sqlite-vec (MKIS On-Device) ────
    val hybridSearchService: com.bioacupunt.biblioteca.data.search.HybridSearchService by lazy {
        com.bioacupunt.biblioteca.data.search.HybridSearchService(
            vecRepo = vecKnowledgeNodeRepository,
            embeddingService = embeddingService,
        )
    }

    // RAG backend = FtsSearchService (FTS4 sobre os 16 artigos fixos revisados + o
    // que a médica aprova na Curadoria). O commit e78f5bf havia trocado para o
    // hybridSearchService (MKIS knowledge_nodes), mas esse store nasce vazio, a perna
    // FTS5 lançava (rank_bm25 inexistente) e a perna vetorial (sqlite-vec + embeddings)
    // não roda no SQLite do framework Android — resultado: AskLibrary respondia
    // NoEvidence para TODA pergunta. Voltamos ao backend que de fato tem acervo.
    // O portão R2 (if (!hasEvidence)) mora no MtcRetriever/AskLibraryUseCase e segue
    // intacto — trocar o backend não o reabre.
    val mtcRetriever: com.bioacupunt.biblioteca.domain.search.MtcRetriever by lazy {
        com.bioacupunt.biblioteca.domain.search.MtcRetriever(ftsSearchService)
    }

    /**
     * The only sanctioned path for asking the AI a knowledge question: it refuses to
     * call the model when the library has no evidence. See AskLibraryUseCase.
     */
    val askLibrary: com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase by lazy {
        com.bioacupunt.biblioteca.domain.usecase.AskLibraryUseCase(mtcRetriever, aiRepository)
    }

    val aiHealthRegistry: com.bioacupunt.ai.health.HealthRegistry by lazy {
        com.bioacupunt.ai.health.DefaultHealthRegistry()
    }
    val generateAiResponse: com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase by lazy {
        com.bioacupunt.ai.domain.usecase.GenerateAiResponseUseCase(aiRepository)
    }

    // ── Inteligência: chat único (RAG-gated com fallback livre) ────
    val appContextBuilder: com.bioacupunt.ai.presentation.AppContextBuilder by lazy {
        com.bioacupunt.ai.presentation.AppContextBuilder(
            appointmentRepository = appointmentRepository,
            securePreferences = securePreferences,
        )
    }

    /**
     * Fábrica do chat único de IA. Toda pergunta passa primeiro por [askLibrary] (o gate R2);
     * o fallback livre ([generateAiResponse]) só é chamado quando a biblioteca não tem
     * evidência. Ver [com.bioacupunt.ai.presentation.UnifiedAiChatViewModel] para o porquê
     * disso é seguro.
     *
     * Função de `patientId`, mesmo padrão de [supremoViewModelFactory]/
     * [evolucaoViewModelFactory] — `0L` (padrão) é o chat geral da bottom nav, sem
     * paciente; `>0` é aberto a partir do Prontuário e escopado por paciente.
     */
    fun unifiedAiChatViewModelFactory(patientId: Long = 0L): com.bioacupunt.ai.presentation.UnifiedAiChatViewModelFactory =
        com.bioacupunt.ai.presentation.UnifiedAiChatViewModelFactory(
            askLibrary = askLibrary,
            generateAiResponse = generateAiResponse,
            contextBuilder = appContextBuilder,
            crmPatientRepository = crmPatientRepository,
            patientId = patientId,
        )

    // ── Seeder ──────────────────────────────────────────────
    private val _seederScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Reset de dados de DEV/TESTE — nunca produção. A mesma checagem que já protege
     * [seedDemoDataIfNeeded] (build precisa ser debuggable) é repetida AQUI DENTRO, não só
     * na tela que chama isto: um botão de UI mal-gateado não deve ser a única coisa entre
     * uma médica de verdade e o prontuário dela sendo apagado (mesmo raciocínio de
     * "filtro de segurança mora dentro da função" do R3/`runnableOn`).
     *
     * Apaga todas as linhas de todas as tabelas (schema intacto) e recarrega SÓ o catálogo
     * ANVISA — que é referência pública real, não dado de clínica. Nenhum paciente,
     * consulta ou transação de demonstração é recriado: o seed de demo foi removido do
     * app (ele era a origem dos "pacientes" Ana Lima/Carlos Souza/Maria Santos e do
     * faturamento fantasma que apareciam no Dashboard e no Financeiro). Os 16 artigos
     * fixos da Biblioteca são constante Kotlin (`MtcKnowledgeBase`), não tabela —
     * sobrevivem ao reset sem re-seed.
     */
    suspend fun resetDevDatabaseIfDebuggable(): Boolean {
        if (!com.bioacupunt.security.AppHardening.isDebugDebuggable(appContext)) return false
        // clearAllTables() é bloqueante (não é suspend) — Room recusa rodar isto na main
        // thread (RoomDatabase.assertNotMainThread), e o chamador (botão em Ajustes, via
        // rememberCoroutineScope) despacha em Dispatchers.Main por padrão. Sem o
        // withContext aqui, a única thread seguinte da coroutine ainda é a main.
        withContext(Dispatchers.IO) { database.clearAllTables() }
        seedPharmaCatalogIfNeeded()
        return true
    }
}
