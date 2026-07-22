package com.bioacupunt.mkis.domain.pipeline

import com.bioacupunt.ai.embedding.EmbeddingService
import com.bioacupunt.data.local.database.IngestionJobDao
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.data.local.database.VecKnowledgeNodeRepository
import com.bioacupunt.data.local.model.IngestionJobEntity
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

/**
 * ORQUESTRADOR DO PIPELINE DE INGESTÃO MKIS.
 *
 * Processa [IngestionJobEntity]s através da máquina de estados, executando
 * cada stage sequencialmente:
 *
 * ```
 * na_fila → processando → classificando → chunk_rodando → embedding_rodando
 *   → indexando → criando_no → concluido
 * ```
 *
 * ## Integração com EmbeddingService
 * Gera embeddings (384d via e5-small) para cada chunk e insere no sqlite-vec.
 *
 * ## Integração com FTS5
 * Indexa título + sumário + conteúdo na virtual table knowledge_fts para busca textual.
 *
 * ## Ciclo de vida
 * - [start] inicia o worker loop em background
 * - [stop] cancela o loop
 * - [ingestContentPack] cria jobs para um pacote de conteúdo
 *
 * ## Thread safety
 * - Worker loop roda em seu próprio [CoroutineScope] com [Dispatchers.IO]
 * - Cada job é processado sequencialmente (sem paralelismo interno)
 * - Jobs concorrentes são processados um por vez (pipeline serial)
 */
class PipelineService(
    private val ingestionJobDao: IngestionJobDao,
    private val knowledgeNodeDao: KnowledgeNodeDao,
    private val vecRepo: VecKnowledgeNodeRepository,
    private val embeddingService: EmbeddingService,
    private val contentExtractor: ContentExtractor,
) {
    // ======================== Estados ========================

    sealed interface PipelineState {
        data object Idle : PipelineState
        data object Running : PipelineState
        data class Processing(val jobId: String, val stage: String, val progress: Float) : PipelineState
        data class Failed(val message: String) : PipelineState
    }

    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: Flow<PipelineState> = _state.asStateFlow()

    /** Flow de progresso: emite a cada mudança de stage. */
    private val _progress = MutableSharedFlow<IngestionProgress>(replay = 0, extraBufferCapacity = 64)
    val progress: Flow<IngestionProgress> = _progress.asSharedFlow()

    // ======================== Worker Loop ========================

    private var workerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Inicia o worker loop em background. Processa jobs da fila continuamente. */
    fun start() {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch {
            _state.value = PipelineState.Running
            AppLogger.i(TAG, "PipelineService iniciado")

            while (isActive) {
                try {
                    processNextJob()
                    delay(POLL_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Erro no worker loop", e)
                    delay(POLL_INTERVAL_MS * 2)
                }
            }
        }
    }

    /** Para o worker loop. */
    fun stop() {
        workerJob?.cancel()
        workerJob = null
        _state.value = PipelineState.Idle
        AppLogger.i(TAG, "PipelineService parado")
    }

    // ======================== Criação de Jobs ========================

    /**
     * Cria jobs de ingestão para um pacote de conteúdo.
     * Cada item tem seu conteúdo armazenado inline (JSON em review_notes)
     * para permitir retry resiliente.
     *
     * @param items Lista de itens a processar
     * @param sourcePackage Nome do pacote de origem (ex: "open_access", "pcdt")
     * @return IDs dos jobs criados
     */
    suspend fun ingestContentPack(
        items: List<ContentPackItem>,
        sourcePackage: String = "manual",
    ): List<String> {
        val jobIds = mutableListOf<String>()

        for (item in items) {
            val jobId = UUID.randomUUID().toString()
            ingestionJobDao.insert(
                IngestionJobEntity(
                    id = jobId,
                    status = "na_fila",
                    source_package = sourcePackage,
                    source_url = item.sourceUrl,
                    review_notes = encodeInlineContent(item.title, item.summary, item.content),
                    created_at = System.currentTimeMillis(),
                )
            )
            jobIds.add(jobId)
        }

        AppLogger.i(TAG, "${jobIds.size} jobs criados do pacote '$sourcePackage'")
        return jobIds
    }

    /** Cria um job único para ingestão manual. */
    suspend fun ingestSingle(
        title: String,
        content: String,
        summary: String = "",
        source: String = "manual",
        sourceUrl: String = "",
    ): String {
        val jobId = UUID.randomUUID().toString()
        val combined = ContentPackItem(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            summary = summary,
            source = source,
            sourceUrl = sourceUrl,
        )

        // Armazena conteúdo inline com formato parseável (para retry resiliente)
        ingestionJobDao.insert(
            IngestionJobEntity(
                id = jobId,
                status = "na_fila",
                source_package = source,
                source_url = sourceUrl,
                review_notes = encodeInlineContent(title, summary, content),
                created_at = System.currentTimeMillis(),
            )
        )

        // Iniciar processamento imediato
        scope.launch { processJob(jobId, combined) }
        return jobId
    }

    // ======================== Processamento ========================

    /** Busca e processa o próximo job na fila. */
    private suspend fun processNextJob() {
        val job = findNextPendingJob() ?: return

        // Buscar conteúdo associado ao job
        val content = resolveContentForJob(job) ?: run {
            ingestionJobDao.fail(job.id, errorMessage = "Conteúdo não encontrado para o job")
            return
        }

        processJob(job.id, content)
    }

    /**
     * Encontra o próximo job pendente (status = 'na_fila').
     * Busca o job mais antigo na fila via @Query no DAO.
     */
    private suspend fun findNextPendingJob(): IngestionJobEntity? {
        return try {
            ingestionJobDao.getNextPendingJob()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Processa um job do início ao fim.
     */
    private suspend fun processJob(jobId: String, content: ContentPackItem) {
        _state.value = PipelineState.Processing(jobId, "iniciando", 0f)
        _progress.emit(IngestionProgress(jobId, "iniciando", 0f, 1f))

        try {
            // Stage 1: Validar
            transitionTo(jobId, "validando", 0.1f)
            val validation = validateContent(content)
            if (validation != null) {
                ingestionJobDao.fail(jobId, errorCode = "VALIDATION_ERROR", errorMessage = validation)
                _progress.emit(IngestionProgress(jobId, "validacao_falhou", 0f, 1f, error = validation))
                return
            }

            // Stage 2: Classificar (via LLM local)
            transitionTo(jobId, "classificando", 0.2f)
            val extraction = contentExtractor.extract(
                title = content.title,
                summary = content.summary,
                content = content.content,
            )

            // Stage 3: Chunking
            transitionTo(jobId, "chunk_rodando", 0.4f)
            val chunks = chunkContent(content)

            // Stage 4: Embedding
            transitionTo(jobId, "embedding_rodando", 0.6f)
            val embeddings = generateEmbeddings(chunks)

            // Stage 5: Criar KnowledgeNode
            transitionTo(jobId, "criando_no", 0.8f)
            val nodeId = createKnowledgeNode(content, extraction)

            // Stage 6: Indexar (FTS5 + sqlite-vec)
            transitionTo(jobId, "indexando", 0.9f)
            indexContent(nodeId, content, chunks, embeddings)

            // Stage 7: Concluir
            ingestionJobDao.complete(jobId, nodeId)
            _state.value = PipelineState.Processing(jobId, "concluido", 1f)
            _progress.emit(IngestionProgress(jobId, "concluido", 1f, 1f))
            AppLogger.i(TAG, "Job $jobId concluído: nó $nodeId")

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Job $jobId falhou", e)
            ingestionJobDao.fail(jobId, errorCode = "PIPELINE_ERROR", errorMessage = e.message)
            _state.value = PipelineState.Processing(jobId, "falhou", 0f)
            _progress.emit(IngestionProgress(jobId, "falhou", 0f, 1f, error = e.message))
        }
    }

    /** Atualiza estado e emite progresso. */
    private suspend fun transitionTo(jobId: String, stage: String, progress: Float) {
        _state.value = PipelineState.Processing(jobId, stage, progress)
        _progress.emit(IngestionProgress(jobId, stage, progress, 1f))
    }

    // ======================== Stages ========================

    /** Valida o conteúdo. Retorna null se OK, ou mensagem de erro. */
    private fun validateContent(content: ContentPackItem): String? {
        if (content.title.isBlank()) return "Título vazio"
        if (content.content.isBlank()) return "Conteúdo vazio"
        if (content.content.length < MIN_CONTENT_LENGTH) return "Conteúdo muito curto (< $MIN_CONTENT_LENGTH caracteres)"
        return null
    }

    /** Divide o conteúdo em chunks para embedding. */
    private fun chunkContent(content: ContentPackItem): List<ContentChunk> {
        val text = "${content.title}\n\n${content.summary}\n\n${content.content}"
        val chunks = mutableListOf<ContentChunk>()

        // Dividir por parágrafos ou a cada CHUNK_SIZE caracteres
        val paragraphs = text.split(Regex("\n\n+"))
        var currentChunk = StringBuilder()
        var chunkIndex = 0

        for (paragraph in paragraphs) {
            if (currentChunk.length + paragraph.length > CHUNK_SIZE && currentChunk.isNotEmpty()) {
                chunks.add(ContentChunk(
                    index = chunkIndex++,
                    text = currentChunk.toString().trim(),
                ))
                currentChunk = StringBuilder()
            }
            if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
            currentChunk.append(paragraph)
        }

        // Último chunk
        if (currentChunk.isNotEmpty()) {
            chunks.add(ContentChunk(
                index = chunkIndex,
                text = currentChunk.toString().trim(),
            ))
        }

        return chunks
    }

    /** Gera embeddings para cada chunk usando o EmbeddingService. */
    private suspend fun generateEmbeddings(chunks: List<ContentChunk>): List<FloatArray> {
        if (!embeddingService.isReady) {
            AppLogger.w(TAG, "EmbeddingService não está pronto. Pulando geração de embeddings.")
            return emptyList()
        }

        return try {
            embeddingService.embedBatch(
                texts = chunks.map { it.text },
                isQuery = false,
                batchSize = 8,
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Falha ao gerar embeddings", e)
            emptyList()
        }
    }

    /** Cria o KnowledgeNodeEntity no banco. */
    private suspend fun createKnowledgeNode(
        content: ContentPackItem,
        extraction: ExtractionResult,
    ): String {
        val now = System.currentTimeMillis()
        val nodeId = UUID.randomUUID().toString()
        val checksum = sha256(content.content)

        val node = KnowledgeNodeEntity(
            id = nodeId,
            tenant_id = "default",
            title = content.title,
            summary = content.summary,
            content = content.content,
            knowledge_type = classifyKnowledgeType(content),
            status = "rascunho",  // Pipeline cria como rascunho — curadoria aprova depois
            evidence_level = extraction.evidenceLevel,
            bias_risk = extraction.biasRisk,
            source = content.source,
            source_url = content.sourceUrl,
            checksum = checksum,
            tags = extraction.keywords.joinToString(" "),
            metadata = """{"pipeline_version": "1.0", "extraction_confidence": ${extraction.confidence}}""",
            created_at = now,
            updated_at = now,
        )

        knowledgeNodeDao.insert(node)
        return nodeId
    }

    /** Indexa conteúdo no FTS5 e embeddings no sqlite-vec. */
    private suspend fun indexContent(
        nodeId: String,
        content: ContentPackItem,
        chunks: List<ContentChunk>,
        embeddings: List<FloatArray>,
    ) {
        // Indexar FTS5 via SQL direto
        indexFts5(nodeId, content)

        // Indexar embeddings no sqlite-vec
        indexEmbeddings(nodeId, chunks, embeddings)
    }

    /** Insere no FTS5 via VecKnowledgeNodeRepository. */
    private suspend fun indexFts5(nodeId: String, content: ContentPackItem) {
        val ok = vecRepo.indexFts5(nodeId, content.title, content.summary, content.content)
        if (ok) {
            AppLogger.i(TAG, "FTS5 indexado para nó $nodeId")
        } else {
            AppLogger.w(TAG, "Falha ao indexar FTS5 para nó $nodeId")
        }
    }

    /** Insere embeddings no sqlite-vec. */
    private suspend fun indexEmbeddings(
        nodeId: String,
        chunks: List<ContentChunk>,
        embeddings: List<FloatArray>,
    ) {
        if (embeddings.isEmpty() || chunks.isEmpty()) return

        val rowId = vecRepo.getRowId(nodeId) ?: return

        // Para o MVP, usamos o embedding do primeiro chunk como representação do nó
        val primaryEmbedding = embeddings.firstOrNull() ?: return
        val blob = embeddingService.floatArrayToBlob(primaryEmbedding)
        vecRepo.upsert(rowId, blob)

        AppLogger.i(TAG, "sqlite-vec indexado para nó $nodeId (rowid=$rowId, dims=${primaryEmbedding.size})")
    }

    // ======================== Utilitários ========================

    /** Classifica o tipo de conhecimento com base no conteúdo. */
    private fun classifyKnowledgeType(content: ContentPackItem): String {
        val text = "${content.title} ${content.summary}".lowercase()
        return when {
            "ensaio clinico" in text || "rct" in text || "randomizado" in text -> "ensaio_clinico"
            "revisao sistematica" in text || "meta-analise" in text || "metanalise" in text -> "revisao"
            "diretriz" in text || "guideline" in text || "protocolo" in text || "pcdt" in text -> "guideline"
            "relato de caso" in text || "caso clinico" in text || "serie de casos" in text -> "caso_clinico"
            "livro" in text || "capitulo" in text || "chapter" in text -> "capitulo"
            "tese" in text || "dissertacao" in text || "doutorado" in text -> "tese"
            "nota tecnica" in text || "comunicado" in text || "editorial" in text -> "nota"
            "educacional" in text || "didatico" in text || "aula" in text || "curso" in text -> "educacional"
            "relatorio" in text || "report" in text || "technical report" in text -> "relatorio"
            else -> "artigo"
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private val inlineJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Conteúdo armazenado inline no review_notes, serializado como JSON. */
    private fun encodeInlineContent(title: String, summary: String, content: String): String {
        val data = InlineContent(title, summary, content)
        return inlineJson.encodeToString(InlineContent.serializer(), data)
    }

    /** Faz o parse do conteúdo inline armazenado em [encodeInlineContent]. */
    private fun decodeInlineContent(raw: String): InlineContent {
        return try {
            inlineJson.decodeFromString(InlineContent.serializer(), raw)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Falha ao fazer parse do conteúdo inline, modo raw: ${e.message}")
            InlineContent(title = "", summary = "", content = raw)
        }
    }

    /** Resolve o conteúdo associado a um job. */
    private suspend fun resolveContentForJob(job: IngestionJobEntity): ContentPackItem? {
        // Tenta extrair conteúdo inline armazenado no review_notes (formato JSON)
        val inline = job.review_notes
        if (!inline.isNullOrBlank()) {
            val parsed = decodeInlineContent(inline)
            return ContentPackItem(
                id = job.id,
                title = parsed.title.ifBlank { "Importação manual" },
                summary = parsed.summary,
                content = parsed.content,
                source = job.source_package ?: "manual",
                sourceUrl = job.source_url ?: "",
            )
        }

        // Jobs com source_url mas sem conteúdo inline precisam de fetch externo
        if (!job.source_url.isNullOrBlank()) {
            return ContentPackItem(
                id = job.id,
                title = "Importado: ${job.source_url}",
                content = "",
                summary = "",
                source = job.source_package ?: "web",
                sourceUrl = job.source_url,
            )
        }

        // Job sem conteúdo nem URL — não pode ser processado
        return null
    }

    companion object {
        private const val TAG = "PipelineService"
        private const val POLL_INTERVAL_MS = 5000L
        private const val CHUNK_SIZE = 1000
        private const val MIN_CONTENT_LENGTH = 50
    }
}

// ======================== Modelos ========================

/** Item de conteúdo para processamento no pipeline. */
data class ContentPackItem(
    val id: String,
    val title: String,
    val content: String,
    val summary: String = "",
    val source: String = "manual",
    val sourceUrl: String = "",
)

/** Chunk de texto para embedding. */
data class ContentChunk(
    val index: Int,
    val text: String,
)

/** Conteúdo inline serializado como JSON no review_notes do job. */
@Serializable
data class InlineContent(
    val title: String = "",
    val summary: String = "",
    val content: String = "",
)

/** Progresso da ingestão. */
data class IngestionProgress(
    val jobId: String,
    val stage: String,
    val progress: Float,
    val total: Float = 1f,
    val error: String? = null,
) {
    val isCompleted: Boolean get() = stage == "concluido"
    val isFailed: Boolean get() = stage == "falhou" || stage.startsWith("validacao_falhou")
    val percent: Int get() = ((progress / total) * 100).toInt().coerceIn(0, 100)
}
