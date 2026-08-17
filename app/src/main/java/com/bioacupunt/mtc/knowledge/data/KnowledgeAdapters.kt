package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.mtc.knowledge.domain.*

private const val MIGRATION_VERSION = "knowledge-core-v1"

private fun importEntity(source: String, originalId: String, type: String, title: String, summary: String, content: String, tags: String, version: String, status: KnowledgeStatus, metadata: Map<String, String> = emptyMap()): KnowledgeImport {
    val now = System.currentTimeMillis()
    val entityType = KnowledgeEntityType.from(type)
    val sourceId = "source.$source.$originalId"
    val citationId = "citation.$source.$originalId"
    val provenance = KnowledgeProvenance(source, originalId, type, migrationVersion = MIGRATION_VERSION, importedAt = now)
    return KnowledgeImport(
        entity = KnowledgeEntity(
            id = KnowledgeCanonicalizer.canonicalId(entityType, title, metadata["canonicalId"]), type = entityType,
            canonicalName = title, aliases = tags.split(',', ';', '|').map { it.trim() }.filter { it.isNotEmpty() },
            summary = summary, content = content, metadata = metadata, sourceIds = listOf(sourceId), citationIds = listOf(citationId),
            version = KnowledgeVersion(version.toString(), now, now, status = status), provenance = listOf(provenance), createdAt = now, updatedAt = now,
        ),
        sources = listOf(KnowledgeSource(sourceId, "$source:$originalId")),
        citations = listOf(KnowledgeCitation(citationId, sourceId)),
    )
}

class LibraryAdapter {
    fun toCanonical(node: BibliotecaNodeEntity): KnowledgeImport = importEntity("library", node.id, node.type, node.title, node.summary, node.content, node.tags, node.version.toString(), KnowledgeStatus.PUBLISHED)
}

class MkisAdapter {
    fun toCanonical(node: KnowledgeNodeEntity): KnowledgeImport = importEntity("mkis", node.id, node.knowledge_type, node.title, node.summary, node.content, node.tags, node.version, status(node.status), mapOf("category" to node.category, "source" to node.source, "checksum" to node.checksum))

    private fun status(value: String): KnowledgeStatus = when (value.lowercase()) {
        "aprovado", "published" -> KnowledgeStatus.PUBLISHED
        "descontinuado", "deprecated" -> KnowledgeStatus.DEPRECATED
        "em_revisao", "review" -> KnowledgeStatus.REVIEW
        else -> KnowledgeStatus.DRAFT
    }
}

/** Explicit adapter name kept for callers that know the legacy table, not its origin pipeline. */
class KnowledgeNodeAdapter {
    private val delegate = MkisAdapter()
    fun toCanonical(node: KnowledgeNodeEntity): KnowledgeImport = delegate.toCanonical(node)
}
