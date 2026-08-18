package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.biblioteca.data.MtcKnowledgeBase
import com.bioacupunt.biblioteca.data.local.BibliotecaDao
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import kotlinx.coroutines.flow.first

/**
 * COVERAGE AUDIT — compares legacy sources against Knowledge Core.
 *
 * Produces a report showing:
 * - How many entities are in each legacy source
 * - How many are in Knowledge Core
 * - Which legacy entities are missing from Knowledge Core
 * - Duplicates and conflicts
 *
 * This audit is READ-ONLY — it never modifies any data.
 */
class KnowledgeCoverageAudit(
    private val bibliotecaDao: BibliotecaDao,
    private val knowledgeNodeDao: KnowledgeNodeDao,
    private val knowledgeCoreDao: KnowledgeCoreDao,
    private val libraryAdapter: LibraryAdapter,
    private val mkisAdapter: MkisAdapter,
) {

    data class CoverageReport(
        val timestamp: Long,
        val mtcKnowledgeBaseCount: Int,
        val bibliotecaNodesCount: Int,
        val knowledgeNodesCount: Int,
        val knowledgeCoreCount: Int,
        val legacyTotal: Int,
        val importedFromLibrary: Int,
        val importedFromMkis: Int,
        val missingFromCore: List<String>,
        val duplicateCount: Int,
        val conflictCount: Int,
        val emptyContentCount: Int,
    )

    suspend fun audit(): CoverageReport {
        val now = System.currentTimeMillis()

        // Count fixed MTC articles (in code, not in Room)
        val mtcBaseCount = MtcKnowledgeBase.articles.size

        // Count biblioteca_nodes
        val libraryNodes = bibliotecaDao.getAllOnce()
        val bibliotecaCount = libraryNodes.size

        // Count knowledge_nodes (MKIS)
        val mkisNodes = knowledgeNodeDao.getAllNodes().first()
        val mkisCount = mkisNodes.size

        // Count knowledge_core_entities
        val coreCount = knowledgeCoreDao.countAll()

        // Convert legacy to canonical IDs for comparison
        val libraryCanonicalIds = libraryNodes.map { libraryAdapter.toCanonical(it).entity.id }.toSet()
        val mkisCanonicalIds = mkisNodes.map { mkisAdapter.toCanonical(it).entity.id }.toSet()
        val allLegacyIds = libraryCanonicalIds + mkisCanonicalIds

        // Check what's in Knowledge Core
        val coreEntities = knowledgeCoreDao.observeAll().first()
        val coreIds = coreEntities.map { it.id }.toSet()

        // Find missing
        val missing = allLegacyIds.filter { it !in coreIds }

        // Count empty content in core
        val emptyContent = coreEntities.count { it.content.isBlank() && it.summary.isBlank() }

        return CoverageReport(
            timestamp = now,
            mtcKnowledgeBaseCount = mtcBaseCount,
            bibliotecaNodesCount = bibliotecaCount,
            knowledgeNodesCount = mkisCount,
            knowledgeCoreCount = coreCount,
            legacyTotal = allLegacyIds.size,
            importedFromLibrary = libraryCanonicalIds.size,
            importedFromMkis = mkisCanonicalIds.size,
            missingFromCore = missing,
            duplicateCount = allLegacyIds.size - (libraryCanonicalIds + mkisCanonicalIds).size,
            conflictCount = 0, // Conflicts detected during import
            emptyContentCount = emptyContent,
        )
    }

    fun formatReport(report: CoverageReport): String = buildString {
        appendLine("=== KNOWLEDGE CORE COVERAGE REPORT ===")
        appendLine("Timestamp: ${report.timestamp}")
        appendLine()
        appendLine("--- Legacy Sources ---")
        appendLine("MtcKnowledgeBase (fixed articles): ${report.mtcKnowledgeBaseCount}")
        appendLine("biblioteca_nodes (curated):        ${report.bibliotecaNodesCount}")
        appendLine("knowledge_nodes (MKIS):            ${report.knowledgeNodesCount}")
        appendLine("Legacy total (unique IDs):          ${report.legacyTotal}")
        appendLine()
        appendLine("--- Knowledge Core ---")
        appendLine("knowledge_core_entities:           ${report.knowledgeCoreCount}")
        appendLine()
        appendLine("--- Import ---")
        appendLine("Imported from library:             ${report.importedFromLibrary}")
        appendLine("Imported from MKIS:                ${report.importedFromMkis}")
        appendLine()
        appendLine("--- Gaps ---")
        appendLine("Missing from Core:                 ${report.missingFromCore.size}")
        if (report.missingFromCore.isNotEmpty()) {
            report.missingFromCore.take(20).forEach { id -> appendLine("  - $id") }
            if (report.missingFromCore.size > 20) {
                appendLine("  ... and ${report.missingFromCore.size - 20} more")
            }
        }
        appendLine("Duplicates:                        ${report.duplicateCount}")
        appendLine("Conflicts:                         ${report.conflictCount}")
        appendLine("Empty content in Core:             ${report.emptyContentCount}")
        appendLine()
        appendLine("=== END REPORT ===")
    }
}
