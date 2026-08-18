package com.bioacupunt.mtc.knowledge.data

import com.bioacupunt.biblioteca.data.local.BibliotecaDao
import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.mtc.knowledge.domain.KnowledgeConflict
import com.bioacupunt.mtc.knowledge.domain.KnowledgeMergeResult
import com.bioacupunt.mtc.knowledge.repository.KnowledgeCoreImporter
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.flow.first

/**
 * LEGACY IMPORT PIPELINE
 *
 * Reads from legacy tables (biblioteca_nodes, knowledge_nodes), converts via
 * adapters to canonical KnowledgeImport, and imports into Knowledge Core.
 *
 * This is a BRIDGE — it does not replace or remove legacy tables. Legacy tables
 * remain the primary source until all consumers are migrated to read from
 * Knowledge Core.
 *
 * Idempotent: re-running does not duplicate knowledge (KnowledgeCanonicalizer
 * merges equivalent entities).
 */
class LegacyImporter(
    private val bibliotecaDao: BibliotecaDao,
    private val knowledgeNodeDao: KnowledgeNodeDao,
    private val importer: KnowledgeCoreImporter,
    private val libraryAdapter: LibraryAdapter,
    private val mkisAdapter: MkisAdapter,
) {
    data class ImportResult(
        val libraryImported: Int,
        val mkisImported: Int,
        val totalEntities: Int,
        val conflicts: List<KnowledgeConflict>,
        val duplicateCount: Int,
    )

    /**
     * Import all legacy data into Knowledge Core.
     * Idempotent — safe to call multiple times.
     */
    suspend fun importAll(): ImportResult {
        AppLogger.i(TAG, "Starting legacy import...")

        // 1. Import from biblioteca_nodes (library articles)
        val libraryNodes = bibliotecaDao.getAllOnce()
        val libraryImports = libraryNodes.map { libraryAdapter.toCanonical(it) }
        AppLogger.i(TAG, "Library: ${libraryNodes.size} nodes → ${libraryImports.size} imports")

        // 2. Import from knowledge_nodes (MKIS) — collect first emission from Flow
        val mkisNodes = knowledgeNodeDao.getAllNodes().first()
        val mkisImports = mkisNodes.map { mkisAdapter.toCanonical(it) }
        AppLogger.i(TAG, "MKIS: ${mkisNodes.size} nodes → ${mkisImports.size} imports")

        // 3. Merge and import all
        val allImports = libraryImports + mkisImports
        val result = if (allImports.isNotEmpty()) {
            importer.import(allImports)
        } else {
            KnowledgeMergeResult(entities = emptyList(), conflicts = emptyList(), duplicateCount = 0)
        }

        AppLogger.i(TAG, "Import complete: ${result.entities.size} entities, " +
            "${result.conflicts.size} conflicts, ${result.duplicateCount} duplicates")

        return ImportResult(
            libraryImported = libraryNodes.size,
            mkisImported = mkisNodes.size,
            totalEntities = result.entities.size,
            conflicts = result.conflicts,
            duplicateCount = result.duplicateCount,
        )
    }

    companion object {
        private const val TAG = "LegacyImporter"
    }
}
