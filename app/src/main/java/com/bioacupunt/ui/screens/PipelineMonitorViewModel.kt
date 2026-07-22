package com.bioacupunt.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.data.local.database.IngestionJobDao
import com.bioacupunt.data.local.model.IngestionJobEntity
import com.bioacupunt.mkis.domain.pipeline.PipelineService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel do monitor do pipeline de ingestão MKIS.
 *
 * Observa todos os jobs em tempo real, o estado do pipeline, e expõe
 * ações de gerenciamento: retry, quarentena, cancelamento, filtros.
 */
class PipelineMonitorViewModel(
    private val ingestionJobDao: IngestionJobDao,
    private val pipelineService: PipelineService,
) : ViewModel() {

    // ======================== Estado ========================

    /** Todos os jobs, ordenados por data descendente. */
    val allJobs: StateFlow<List<IngestionJobEntity>> = ingestionJobDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Jobs em quarentena. */
    val quarantinedJobs: StateFlow<List<IngestionJobEntity>> = ingestionJobDao.getQuarantined()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Estado do pipeline. */
    val pipelineState: StateFlow<PipelineService.PipelineState> = pipelineService.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipelineService.PipelineState.Idle)

    /** Progresso da ingestão atual (eventos não replicados). */
    val pipelineProgress: Flow<com.bioacupunt.mkis.domain.pipeline.IngestionProgress> =
        pipelineService.progress

    // ======================== Filtros ========================

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    /** Jobs filtrados por status + busca. */
    val filteredJobs: StateFlow<List<IngestionJobEntity>> = combine(
        allJobs, statusFilter, searchText
    ) { jobs, filter, query ->
        jobs.filter { job ->
            val matchesFilter = filter == null || job.status == filter
            val matchesSearch = query.isBlank() ||
                job.id.contains(query, ignoreCase = true) ||
                (job.source_package?.contains(query, ignoreCase = true) == true) ||
                (job.error_message?.contains(query, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ======================== Estatísticas ========================

    /** Resumo estatístico dos jobs. */
    data class JobStats(
        val total: Int = 0,
        val pending: Int = 0,
        val processing: Int = 0,
        val completed: Int = 0,
        val failed: Int = 0,
        val quarantined: Int = 0,
        val cancelled: Int = 0,
    )

    val stats: StateFlow<JobStats> = allJobs.map { jobs ->
        JobStats(
            total = jobs.size,
            pending = jobs.count { it.status == "na_fila" },
            processing = jobs.count { it.status in PROCESSING_STATES },
            completed = jobs.count { it.status == "concluido" },
            failed = jobs.count { it.status in FAILED_STATES || it.status == "falhou" },
            quarantined = jobs.count { it.status == "em_quarentena" },
            cancelled = jobs.count { it.status in setOf("cancelado", "bloqueado_manualmente") },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JobStats())

    // ======================== Ações ========================

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun retryJob(jobId: String) {
        viewModelScope.launch {
            val newAttemptId = UUID.randomUUID().toString()
            ingestionJobDao.resetForRetry(jobId, newAttemptId)
        }
    }

    fun quarantineJob(jobId: String, reason: String = "Quarentena manual pela médica") {
        viewModelScope.launch {
            ingestionJobDao.quarantine(jobId, reason)
        }
    }

    fun cancelJob(jobId: String) {
        viewModelScope.launch {
            ingestionJobDao.updateStatus(jobId, "cancelado")
        }
    }

    fun startPipeline() {
        pipelineService.start()
    }

    fun stopPipeline() {
        pipelineService.stop()
    }

    /** Helper: formata timestamp para exibição. */
    fun formatTimestamp(millis: Long): String {
        return DATE_FMT.format(java.util.Date(millis))
    }

    /** Helper: label amigável para cada status. */
    fun statusLabel(status: String): String = when (status) {
        "na_fila" -> "Na fila"
        "baixando" -> "Baixando"
        "escanando" -> "Escaneando"
        "validacao_falhou" -> "Validação falhou"
        "scan_falhou" -> "Scan falhou"
        "em_quarentena" -> "Em quarentena"
        "ocr_na_fila", "ocr_rodando" -> "OCR"
        "ocr_falhou" -> "OCR falhou"
        "parse_na_fila", "parse_rodando" -> "Parse"
        "parse_falhou" -> "Parse falhou"
        "chunk_na_fila", "chunk_rodando" -> "Chunking"
        "chunk_falhou" -> "Chunking falhou"
        "embedding_na_fila", "embedding_rodando" -> "Embedding"
        "embedding_falhou" -> "Embedding falhou"
        "indexacao_na_fila", "indexacao_rodando" -> "Indexação"
        "indexacao_falhou" -> "Indexação falhou"
        "criando_no" -> "Criando nó"
        "criacao_falhou" -> "Criação falhou"
        "revisao_necessaria" -> "Revisão necessária"
        "concluido" -> "Concluído"
        "falhou" -> "Falhou"
        "bloqueado_manualmente" -> "Bloqueado"
        "cancelado" -> "Cancelado"
        else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    companion object {
        private val DATE_FMT = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("pt", "BR"))

        val PROCESSING_STATES = setOf(
            "baixando", "escanando", "ocr_na_fila", "ocr_rodando",
            "parse_na_fila", "parse_rodando", "chunk_na_fila", "chunk_rodando",
            "embedding_na_fila", "embedding_rodando", "indexacao_na_fila",
            "indexacao_rodando", "criando_no",
        )
        val FAILED_STATES = setOf(
            "validacao_falhou", "scan_falhou", "ocr_falhou", "parse_falhou",
            "chunk_falhou", "embedding_falhou", "indexacao_falhou", "criacao_falhou",
        )
    }
}

/** Factory para [PipelineMonitorViewModel]. */
class PipelineMonitorViewModelFactory(
    private val ingestionJobDao: IngestionJobDao,
    private val pipelineService: PipelineService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PipelineMonitorViewModel::class.java)) {
            return PipelineMonitorViewModel(ingestionJobDao, pipelineService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
