package com.bioacupunt.mtc.knowledge.data

/**
 * FTS5 VIRTUAL TABLE — full-text search index for Knowledge Core entities.
 *
 * This class represents the FTS5 virtual table structure for type-safe
 * mapping from search results. The actual table is created via raw SQL
 * in MIGRATION_25_26 (Room's @Fts5 annotation has KSP issues in this config).
 *
 * The FTS5 table is created as:
 * ```sql
 * CREATE VIRTUAL TABLE knowledge_core_fts USING fts5(
 *     canonical_name,
 *     aliases,
 *     summary,
 *     content
 * )
 * ```
 *
 * Populated via raw SQL INSERT from [KnowledgeCoreFtsSyncer].
 * Searched via raw SQL queries from [KnowledgeSearchRepository].
 *
 * This is an INDEX, not a source of truth.
 * Canonical data lives in `knowledge_core_entities`.
 */
data class KnowledgeCoreFtsEntity(
    val canonicalName: String,
    val aliases: String = "",
    val summary: String = "",
    val content: String = "",
)
