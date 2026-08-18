package com.bioacupunt.mtc.knowledge

import com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity
import com.bioacupunt.data.local.model.KnowledgeNodeEntity
import com.bioacupunt.mtc.knowledge.data.LibraryAdapter
import com.bioacupunt.mtc.knowledge.data.MkisAdapter
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus
import org.junit.Assert.*
import org.junit.Test

class KnowledgeAdapterTest {

    private val libraryAdapter = LibraryAdapter()
    private val mkisAdapter = MkisAdapter()

    // ── LibraryAdapter ────────────────────────────────────────────────

    @Test
    fun libraryAdapter_convertsBasicFields() {
        val node = BibliotecaNodeEntity(
            id = "art-001",
            type = "PATTERN",
            title = "Estagnação de Qi",
            content = "Conteúdo completo",
            summary = "Resumo do artigo",
            tags = "fígado,qi,estagnação",
            version = 3,
            metadata = "{}",
        )

        val import = libraryAdapter.toCanonical(node)

        assertEquals(KnowledgeEntityType.PATTERN, import.entity.type)
        assertEquals("Estagnação de Qi", import.entity.canonicalName)
        assertEquals("Resumo do artigo", import.entity.summary)
        assertEquals("Conteúdo completo", import.entity.content)
        assertEquals(KnowledgeStatus.PUBLISHED, import.entity.version.status)
        assertEquals(3, import.entity.version.version.toInt())
    }

    @Test
    fun libraryAdapter_preservesProvenance() {
        val node = BibliotecaNodeEntity(
            id = "art-002",
            type = "DOCUMENT",
            title = "Artigo Teste",
            content = "",
            summary = "",
            tags = "",
            version = 1,
            metadata = "{}",
        )

        val import = libraryAdapter.toCanonical(node)

        assertEquals(1, import.entity.provenance.size)
        assertEquals("library", import.entity.provenance[0].originalSource)
        assertEquals("art-002", import.entity.provenance[0].originalId)
        assertEquals("knowledge-core-v1", import.entity.provenance[0].migrationVersion)
    }

    @Test
    fun libraryAdapter_createsSourceAndCitation() {
        val node = BibliotecaNodeEntity(
            id = "art-003",
            type = "ACUPOINT",
            title = "LI4",
            content = "",
            summary = "",
            tags = "",
            version = 1,
            metadata = "{}",
        )

        val import = libraryAdapter.toCanonical(node)

        assertEquals(1, import.sources.size)
        assertEquals("source.library.art-003", import.sources[0].id)
        assertEquals(1, import.citations.size)
        assertEquals("citation.library.art-003", import.citations[0].id)
        assertEquals("source.library.art-003", import.citations[0].sourceId)
    }

    @Test
    fun libraryAdapter_parsesTagsAsAliases() {
        val node = BibliotecaNodeEntity(
            id = "art-004",
            type = "HERB",
            title = "Chai Hu",
            content = "",
            summary = "",
            tags = "柴胡, Bupleurum, Hare Ear",
            version = 1,
            metadata = "{}",
        )

        val import = libraryAdapter.toCanonical(node)

        assertEquals(3, import.entity.aliases.size)
        assertTrue(import.entity.aliases.contains("柴胡"))
        assertTrue(import.entity.aliases.contains("Bupleurum"))
        assertTrue(import.entity.aliases.contains("Hare Ear"))
    }

    // ── MkisAdapter ───────────────────────────────────────────────────

    @Test
    fun mkisAdapter_convertsBasicFields() {
        val node = KnowledgeNodeEntity(
            id = "mkis-001",
            knowledge_type = "SYMPTOM",
            title = "Insônia",
            summary = "Distúrbio do sono",
            content = "Detalhes...",
            tags = "sono,insônia",
            version = "1.0",
            status = "aprovado",
            category = "sintoma",
            source = "Maciocia",
            checksum = "abc123",
            tenant_id = "default",
            created_at = System.currentTimeMillis(),
        )

        val import = mkisAdapter.toCanonical(node)

        assertEquals(KnowledgeEntityType.SYMPTOM, import.entity.type)
        assertEquals("Insônia", import.entity.canonicalName)
        assertEquals(KnowledgeStatus.PUBLISHED, import.entity.version.status)
        assertEquals("Maciocia", import.entity.metadata["source"])
        assertEquals("abc123", import.entity.metadata["checksum"])
    }

    @Test
    fun mkisAdapter_statusMapping() {
        fun statusOf(status: String): KnowledgeStatus {
            val node = KnowledgeNodeEntity(
                id = "x", knowledge_type = "THEORY", title = "T", summary = "", content = "",
                tags = "", version = "1", status = status, category = "", source = "",
                checksum = "", tenant_id = "default", created_at = 0,
            )
            return mkisAdapter.toCanonical(node).entity.version.status
        }

        assertEquals(KnowledgeStatus.PUBLISHED, statusOf("aprovado"))
        assertEquals(KnowledgeStatus.PUBLISHED, statusOf("published"))
        assertEquals(KnowledgeStatus.DEPRECATED, statusOf("descontinuado"))
        assertEquals(KnowledgeStatus.DEPRECATED, statusOf("deprecated"))
        assertEquals(KnowledgeStatus.REVIEW, statusOf("em_revisao"))
        assertEquals(KnowledgeStatus.REVIEW, statusOf("review"))
        assertEquals(KnowledgeStatus.DRAFT, statusOf("rascunho"))
        assertEquals(KnowledgeStatus.DRAFT, statusOf("unknown"))
    }

    @Test
    fun mkisAdapter_preservesProvenance() {
        val node = KnowledgeNodeEntity(
            id = "mkis-002",
            knowledge_type = "PATTERN",
            title = "Padrão Teste",
            summary = "",
            content = "",
            tags = "",
            version = "2.0",
            status = "aprovado",
            category = "",
            source = "",
            checksum = "",
            tenant_id = "default",
            created_at = 0,
        )

        val import = mkisAdapter.toCanonical(node)

        assertEquals(1, import.entity.provenance.size)
        assertEquals("mkis", import.entity.provenance[0].originalSource)
        assertEquals("mkis-002", import.entity.provenance[0].originalId)
    }
}
