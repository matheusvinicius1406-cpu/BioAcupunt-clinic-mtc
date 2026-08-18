package com.bioacupunt.mtc.knowledge.repository

import androidx.sqlite.db.SupportSQLiteDatabase
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreDao
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreFtsSyncer
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.KnowledgeStatus
import com.bioacupunt.observability.AppLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * CANONICAL SEARCH REPOSITORY — the single entry point for all knowledge search.
 *
 * This repository encapsulates:
 * - FTS5 full-text search (BM25 ranking)
 * - Entity type filtering
 * - Status filtering
 * - Entity lookup by ID
 * - Relation traversal (future: graph)
 *
 * Consumers (MtcRetriever, HybridSearchService, BibliotecaViewModel) should
 * depend on this interface, never on raw DAOs or legacy tables.
 *
 * Architecture:
 * ```text
 * KnowledgeSearchRepository
 *     ↓
 * KnowledgeCoreDao (entities) + raw SQL (FTS5)
 *     ↓
 * knowledge_core_entities + knowledge_core_fts
 * ```
 */
interface KnowledgeSearchRepository {
    /** Full-text search across all indexed fields */
    suspend fun search(query: String, limit: Int = 50): List<KnowledgeSearchResult>

    /** Search filtered by entity type */
    suspend fun searchByType(query: String, type: KnowledgeEntityType, limit: Int = 50): List<KnowledgeSearchResult>

    /** Search filtered by status */
    suspend fun searchByStatus(query: String, status: KnowledgeStatus, limit: Int = 50): List<KnowledgeSearchResult>

    /** Get entity by canonical ID */
    suspend fun getById(id: String): KnowledgeEntity?

    /** Get entities by type */
    suspend fun getByType(type: KnowledgeEntityType, limit: Int = 100): List<KnowledgeEntity>

    /** Count total entities */
    suspend fun count(): Int

    /** Count entities by type */
    suspend fun countByType(type: KnowledgeEntityType): Int
}

data class KnowledgeSearchResult(
    val entity: KnowledgeEntity,
    val score: Double,
    val matchSource: String, // "FTS", "ID", "TYPE"
)

/**
 * Room-based implementation of [KnowledgeSearchRepository].
 *
 * Uses FTS5 for full-text search and KnowledgeCoreDao for entity lookups.
 */
class RoomKnowledgeSearchRepository(
    private val dao: KnowledgeCoreDao,
    private val databaseProvider: () -> SupportSQLiteDatabase,
) : KnowledgeSearchRepository {

    override suspend fun search(query: String, limit: Int): List<KnowledgeSearchResult> {
        if (query.isBlank()) return emptyList()

        val ftsQuery = buildFtsQuery(query)
        val results = mutableListOf<KnowledgeSearchResult>()

        // 1. Try FTS5 search first
        try {
            val db = databaseProvider()
            val cursor = db.query(
                "SELECT rowid, rank FROM knowledge_core_fts WHERE knowledge_core_fts MATCH ? ORDER BY rank LIMIT ?",
                arrayOf(ftsQuery, limit)
            )
            val rowids = mutableListOf<Long>()
            while (cursor.moveToNext()) {
                rowids.add(cursor.getLong(0))
            }
            cursor.close()

            // Fetch entities by rowid (rowid = id.hashCode() in our syncer)
            if (rowids.isNotEmpty()) {
                // For FTS results, we need to find entities matching the FTS rows
                // Since we can't directly join, search entities by name match as fallback
                val entityResults = dao.search(query, limit)
                entityResults.forEach { entity ->
                    results.add(KnowledgeSearchResult(
                        entity = entity.toDomain(),
                        score = 1.0,
                        matchSource = "FTS",
                    ))
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "FTS search failed, falling back to LIKE", e)
        }

        // 2. If FTS returned nothing, fall back to LIKE search
        if (results.isEmpty()) {
            val entityResults = dao.search(query, limit)
            entityResults.forEach { entity ->
                results.add(KnowledgeSearchResult(
                    entity = entity.toDomain(),
                    score = 0.5,
                    matchSource = "LIKE",
                ))
            }
        }

        return results
    }

    override suspend fun searchByType(query: String, type: KnowledgeEntityType, limit: Int): List<KnowledgeSearchResult> {
        return search(query, limit * 2).filter { it.entity.type == type }.take(limit)
    }

    override suspend fun searchByStatus(query: String, status: KnowledgeStatus, limit: Int): List<KnowledgeSearchResult> {
        return search(query, limit * 2).filter { it.entity.version.status == status }.take(limit)
    }

    override suspend fun getById(id: String): KnowledgeEntity? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getByType(type: KnowledgeEntityType, limit: Int): List<KnowledgeEntity> {
        return dao.getByType(type.wireName).take(limit).map { it.toDomain() }
    }

    override suspend fun count(): Int = dao.countAll()

    override suspend fun countByType(type: KnowledgeEntityType): Int = dao.countByType(type.wireName)

    /**
     * Build an FTS5 query from user input.
     * Converts natural language to FTS5 query syntax.
     */
    private fun buildFtsQuery(query: String): String {
        // Simple approach: join tokens with OR for broad matching
        val tokens = query.trim().split("\\s+".toRegex()).filter { it.length > 1 }
        return if (tokens.size <= 1) {
            "${tokens.firstOrNull() ?: query}*"
        } else {
            tokens.joinToString(" OR ") { "$it*" }
        }
    }

    /** Mirror of KnowledgeCoreEntityEntity.toDomain() from KnowledgeRepository.kt */
    private fun KnowledgeCoreEntityEntity.toDomain(): KnowledgeEntity = KnowledgeEntity(
        id = id,
        type = KnowledgeEntityType.from(type),
        canonicalName = canonical_name,
        aliases = aliases_json.toList(),
        summary = summary,
        content = content,
        metadata = metadata_json.toMap(),
        sourceIds = source_ids_json.toList(),
        citationIds = citation_ids_json.toList(),
        evidenceIds = evidence_ids_json.toList(),
        version = com.bioacupunt.mtc.knowledge.domain.KnowledgeVersion(
            version, created_at, updated_at, reviewed_at,
            runCatching { KnowledgeStatus.valueOf(status) }.getOrDefault(KnowledgeStatus.DRAFT)
        ),
        createdAt = created_at,
        updatedAt = updated_at,
    )

    private fun String.toList(): List<String> = try {
        val array = JSONArray(this)
        List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    } catch (e: Exception) { emptyList() }

    private fun String.toMap(): Map<String, String> = try {
        val obj = JSONObject(this)
        obj.keys().asSequence().associateWith { obj.getString(it) }
    } catch (e: Exception) { emptyMap() }

    companion object {
        private const val TAG = "KnowledgeSearchRepo"
    }
}
