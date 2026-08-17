package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeCanonicalizerTest {
    @Test fun normalizesAccentsAndSeparators() {
        assertEquals("estagnacao_de_qi_do_figado", KnowledgeCanonicalizer.normalizeName("Estagnação de Qi do Fígado"))
    }

    @Test fun sameTypeAndNameConverge() {
        assertEquals("pattern.estagnacao_de_qi", KnowledgeCanonicalizer.canonicalId(KnowledgeEntityType.PATTERN, "Estagnação de Qi"))
    }

    @Test fun equivalentImportsAreMergedAndProvenanceIsPreserved() {
        val now = 1L
        fun item(source: String, content: String) = KnowledgeImport(KnowledgeEntity("pattern.x", KnowledgeEntityType.PATTERN, "Padrão X", content = content, version = KnowledgeVersion("1", now, now), provenance = listOf(KnowledgeProvenance(source, source, "pattern", migrationVersion = "test", importedAt = now))))
        val merged = KnowledgeCanonicalizer.merge(listOf(item("library", "mesmo"), item("mkis", "mesmo")))
        assertEquals(1, merged.entities.size)
        assertEquals(1, merged.duplicateCount)
        assertEquals(2, merged.entities.single().provenance.size)
        assertTrue(merged.conflicts.isEmpty())
    }

    @Test fun divergentContentIsConflictNotDeletion() {
        val now = 1L
        fun item(source: String, content: String) = KnowledgeImport(KnowledgeEntity("pattern.x", KnowledgeEntityType.PATTERN, "Padrão X", content = content, version = KnowledgeVersion("1", now, now), provenance = listOf(KnowledgeProvenance(source, source, "pattern", migrationVersion = "test", importedAt = now))))
        val merged = KnowledgeCanonicalizer.merge(listOf(item("library", "A"), item("mkis", "B")))
        assertEquals(1, merged.entities.size)
        assertEquals(1, merged.conflicts.size)
        assertEquals(2, merged.entities.single().provenance.size)
    }

    @Test fun equivalentAliasesConvergeEvenWithDifferentSourceIds() {
        val now = 1L
        val a = KnowledgeImport(KnowledgeEntity("library.a", KnowledgeEntityType.PATTERN, "Padrão hepático", aliases = listOf("Qi do Fígado"), version = KnowledgeVersion("1", now, now)))
        val b = KnowledgeImport(KnowledgeEntity("mkis.b", KnowledgeEntityType.PATTERN, "Estagnação de Qi do Fígado", aliases = listOf("Qi do Fígado"), version = KnowledgeVersion("1", now, now)))
        val merged = KnowledgeCanonicalizer.merge(listOf(a, b))
        assertEquals(1, merged.entities.size)
        assertEquals(1, merged.duplicateCount)
    }
}
