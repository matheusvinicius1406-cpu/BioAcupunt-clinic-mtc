package com.bioacupunt.mtc.knowledge.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.bioacupunt.observability.AppLogger
import org.json.JSONArray

/**
 * FTS5 SYNCER — keeps the knowledge_core_fts virtual table in sync with
 * knowledge_core_entities.
 *
 * The FTS5 table is populated via raw SQL (Room's @Fts5 annotation has
 * KSP issues in this project configuration). This class handles:
 *
 * 1. Full rebuild: delete all FTS entries, re-insert from entities
 * 2. Incremental update: insert/update/delete individual entities
 *
 * Called after:
 * - LegacyImporter.importAll()
 * - Manual knowledge base updates
 * - App startup (if FTS is empty)
 *
 * The FTS table is an INDEX, not a source of truth.
 */
class KnowledgeCoreFtsSyncer(
    private val databaseProvider: () -> SupportSQLiteDatabase,
) {

    /**
     * Full rebuild of the FTS index from knowledge_core_entities.
     * Safe to call multiple times (idempotent).
     */
    fun rebuildFull() {
        val db = databaseProvider()
        try {
            // Clear existing FTS entries
            db.execSQL("DELETE FROM knowledge_core_fts")

            // Read all entities and insert into FTS
            val cursor = db.query("SELECT id, canonical_name, aliases_json, summary, content FROM knowledge_core_entities")
            var count = 0
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val canonicalName = cursor.getString(1) ?: ""
                val aliasesJson = cursor.getString(2) ?: "[]"
                val summary = cursor.getString(3) ?: ""
                val content = cursor.getString(4) ?: ""

                // Convert aliases JSON array to space-separated string for FTS
                val aliases = jsonToArray(aliasesJson).joinToString(" ")

                // Insert into FTS (FTS4 manages rowid implicitly)
                db.execSQL(
                    "INSERT INTO knowledge_core_fts(canonical_name, aliases, summary, content) VALUES (?, ?, ?, ?)",
                    arrayOf(canonicalName, aliases, summary, content)
                )
                count++
            }
            cursor.close()
            AppLogger.i(TAG, "FTS rebuild complete: $count entries indexed")
        } catch (e: Exception) {
            AppLogger.e(TAG, "FTS rebuild failed", e)
        }
    }

    /**
     * Rebuild FTS for a single entity (incremental update).
     */
    fun syncEntity(entityId: String, canonicalName: String, aliasesJson: String, summary: String, content: String) {
        val db = databaseProvider()
        try {
            // Find existing FTS rowid for this entity
            val cursor = db.query(
                "SELECT rowid FROM knowledge_core_fts WHERE canonical_name = ?",
                arrayOf(canonicalName)
            )
            val aliases = jsonToArray(aliasesJson).joinToString(" ")

            if (cursor.moveToFirst()) {
                val rowid = cursor.getLong(0)
                cursor.close()
                // Update existing
                db.execSQL(
                    "UPDATE knowledge_core_fts SET canonical_name = ?, aliases = ?, summary = ?, content = ? WHERE rowid = ?",
                    arrayOf(canonicalName, aliases, summary, content, rowid.toString())
                )
            } else {
                cursor.close()
                // Insert new
                db.execSQL(
                    "INSERT INTO knowledge_core_fts(canonical_name, aliases, summary, content) VALUES (?, ?, ?, ?)",
                    arrayOf(canonicalName, aliases, summary, content)
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "FTS sync failed for entity $entityId", e)
        }
    }

    /**
     * Remove an entity from the FTS index.
     */
    fun removeEntity(canonicalName: String) {
        val db = databaseProvider()
        try {
            db.execSQL(
                "DELETE FROM knowledge_core_fts WHERE canonical_name = ?",
                arrayOf(canonicalName)
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "FTS remove failed for $canonicalName", e)
        }
    }

    /** Count entries in the FTS index */
    fun count(): Int {
        val db = databaseProvider()
        val cursor = db.query("SELECT COUNT(*) FROM knowledge_core_fts")
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    /** Check if FTS index is empty (needs initial sync) */
    fun isEmpty(): Boolean = count() == 0

    private fun jsonToArray(json: String): List<String> = try {
        val array = JSONArray(json)
        List(array.length()) { i -> array.optString(i) }.filter { it.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    companion object {
        private const val TAG = "KnowledgeCoreFtsSyncer"
    }
}
