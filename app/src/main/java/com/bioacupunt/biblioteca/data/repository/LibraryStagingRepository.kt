package com.bioacupunt.biblioteca.data.repository

import com.bioacupunt.biblioteca.data.local.BibliotecaDao
import com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack
import com.bioacupunt.biblioteca.domain.ingestion.LibraryIngestion
import com.bioacupunt.biblioteca.domain.ingestion.ReviewMeta
import com.bioacupunt.biblioteca.domain.ingestion.ReviewStatus
import com.bioacupunt.biblioteca.domain.model.MtcArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistência do pipeline de ingestão sobre a tabela `biblioteca_nodes` que já existe.
 *
 * O estado de revisão + proveniência mora no campo `metadata` (JSON [ReviewMeta]), então
 * não há migração de schema. O restante do app enxerga um nó encenado como qualquer outro
 * nó da biblioteca; a diferença — pendente vs aprovado — está no metadata, lido em Kotlin.
 */
class LibraryStagingRepository(
    private val dao: BibliotecaDao,
) {
    // Json próprio e tolerante: metadata legado (nós sem ReviewMeta) não pode derrubar a
    // leitura — degrada para "sem revisão" e segue, no espírito do resto do app clínico.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun metaOf(entity: BibliotecaNodeEntity): ReviewMeta? =
        runCatching { json.decodeFromString<ReviewMeta>(entity.metadata) }.getOrNull()

    private fun toEntity(node: LibraryIngestion.StagedNode): BibliotecaNodeEntity =
        BibliotecaNodeEntity(
            id = node.article.id,
            // `type` carrega a categoria MTC do artigo — é assim que toArticle a recupera.
            type = node.article.category,
            title = node.article.title,
            content = node.article.content,
            summary = node.article.summary,
            tags = node.article.tags.joinToString(","),
            version = 1,
            metadata = json.encodeToString(node.meta),
        )

    // A categoria do artigo encenado é guardada no campo `type` da entity (ver stagePack),
    // então recuperá-la é direto — sem heurística.
    private fun toArticle(entity: BibliotecaNodeEntity): MtcArticle =
        MtcArticle(
            id = entity.id,
            title = entity.title,
            category = entity.type,
            summary = entity.summary,
            content = entity.content,
            tags = entity.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )

    /**
     * Encena um pacote curado. Só itens que passam pelo portão [LibraryIngestion] entram,
     * como PENDING. Retorna o resultado para a UI mostrar quantos entraram/foram rejeitados.
     * Idempotente por id (REPLACE) — reprocessar o mesmo pacote não duplica.
     */
    suspend fun stagePack(pack: LibraryContentPack, now: Long): LibraryIngestion.Outcome {
        val outcome = LibraryIngestion.stage(pack, now)
        if (outcome.staged.isNotEmpty()) {
            // Não re-encena o que já foi revisado: se o id já existe e não está PENDING,
            // preserva a decisão da médica.
            val existing = dao.getAllOnce().associateBy { it.id }
            val toInsert = outcome.staged.filter { node ->
                val prev = existing[node.article.id]?.let(::metaOf)
                prev == null || prev.status == ReviewStatus.PENDING
            }.map(::toEntity)
            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        }
        return outcome
    }

    /** Fila de revisão: só os pendentes, para a médica decidir. */
    fun observePending(): Flow<List<StagedArticle>> =
        dao.observeAll().map { list ->
            list.mapNotNull { e ->
                val meta = metaOf(e) ?: return@mapNotNull null
                if (meta.status != ReviewStatus.PENDING) null
                else StagedArticle(toArticle(e), meta)
            }
        }

    /** Snapshot dos artigos APROVADOS — o que pode entrar no acervo consultável/RAG. */
    suspend fun approvedArticles(): List<MtcArticle> =
        dao.getAllOnce().filter { metaOf(it)?.status == ReviewStatus.APPROVED }.map(::toArticle)

    suspend fun approve(id: String, now: Long) = transition(id, ReviewStatus.APPROVED, now)
    suspend fun reject(id: String, now: Long) = transition(id, ReviewStatus.REJECTED, now)

    private suspend fun transition(id: String, status: ReviewStatus, now: Long) {
        val entity = dao.getById(id) ?: return
        val meta = metaOf(entity) ?: return
        val updated = entity.copy(metadata = json.encodeToString(meta.copy(status = status, reviewedAt = now)))
        dao.insertAll(listOf(updated))
    }

    data class StagedArticle(val article: MtcArticle, val meta: ReviewMeta)
}
