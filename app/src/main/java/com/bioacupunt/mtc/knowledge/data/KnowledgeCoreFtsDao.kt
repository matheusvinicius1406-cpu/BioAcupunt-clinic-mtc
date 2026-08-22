package com.bioacupunt.mtc.knowledge.data

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * FTS5 DAO — raw SQL queries against knowledge_core_fts.
 *
 * This is NOT a Room DAO (the FTS5 virtual table is created via raw SQL
 * in MIGRATION_25_26, not as a Room entity). It provides type-safe access
 * to the FTS5 index for search operations.
 *
 * Used by [KnowledgeSearchRepository] for full-text search queries.
 */
class KnowledgeCoreFtsDao(
    private val databaseProvider: () -> SupportSQLiteDatabase,
) {

    /**
     * Full-text search with BM25 ranking.
     *
     * @param query FTS5 query string (supports terms, phrases, prefixes, AND/OR/NOT)
     * @param limit Maximum results
     * @return List of (rowid, rank) pairs ordered by relevance
     */
    fun search(query: String, limit: Int = 50): List<FtsSearchResult> {
        val db = databaseProvider()
        val results = mutableListOf<FtsSearchResult>()
        try {
            val cursor = db.query(
                "SELECT rowid, rank FROM knowledge_core_fts WHERE knowledge_core_fts MATCH ? ORDER BY rank LIMIT ?",
                arrayOf<String?>(query, limit.toString())
            )
            while (cursor.moveToNext()) {
                results.add(FtsSearchResult(
                    rowid = cursor.getLong(0),
                    rank = cursor.getDouble(1),
                ))
            }
            cursor.close()
        } catch (e: Exception) {
            // FTS query syntax error or table not ready — return empty
        }
        return results
    }

    /** Count indexed entries */
    fun count(): Int {
        val db = databaseProvider()
        val cursor = db.query("SELECT COUNT(*) FROM knowledge_core_fts")
        val count: Int = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /** Check if FTS index is empty */
    fun isEmpty(): Boolean = count() == 0

    /** Clear all FTS entries */
    fun deleteAll() {
        databaseProvider().execSQL("DELETE FROM knowledge_core_fts")
    }
}

/** Result of an FTS search */
data class FtsSearchResult(
    val rowid: Long,
    val rank: Double = 0.0,
)
