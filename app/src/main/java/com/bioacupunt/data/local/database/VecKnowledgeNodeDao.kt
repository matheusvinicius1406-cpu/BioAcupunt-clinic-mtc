package com.bioacupunt.data.local.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteProgram
import com.bioacupunt.data.local.model.KnowledgeNodeEntity

/**
 * REPOSITÓRIO DE OPERAÇÕES sqlite-vec.
 *
 * Em vez de usar @RawQuery do Room (que tem problemas com o KSP para
 * interfaces SupportSQLiteQuery anônimas), usamos o [SupportSQLiteDatabase]
 * diretamente. O acesso ao database é obtido via [AppDatabase.openHelper].
 *
 * ## Uso típico:
 * ```kotlin
 * val repo = VecKnowledgeNodeRepository(database.openHelper.writableDatabase)
 * repo.upsert(nodeId, embeddingBlob)
 * val results = repo.search(queryBlob, limit = 10)
 * ```
 */
class VecKnowledgeNodeRepository(private val db: SupportSQLiteDatabase) {

    // ======================== CRUD ========================

    /**
     * Insere ou atualiza o embedding de um knowledge_node.
     * @return true se bem-sucedido
     */
    fun upsert(rowId: Long, embeddingBlob: ByteArray): Boolean {
        return try {
            db.execSQL(
                "INSERT OR REPLACE INTO vec_knowledge_nodes(rowid, embedding) VALUES(?, ?)",
                arrayOf(rowId, embeddingBlob),
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Remove o embedding de um nó.
     * @return true se removeu ao menos 1 linha
     */
    fun delete(rowId: Long): Boolean {
        return try {
            db.execSQL(
                "DELETE FROM vec_knowledge_nodes WHERE rowid = ?",
                arrayOf(rowId),
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ======================== Busca ========================

    /**
     * Busca os top-k vizinhos por similaridade coseno.
     *
     * @param embeddingBlob Embedding da query (Float32[384] em little-endian)
     * @param limit Número de resultados (k)
     * @param statusFilter Se não-nulo, filtra por status do nó
     * @return Lista de resultados ordenados por distância crescente
     */
    fun search(
        embeddingBlob: ByteArray,
        limit: Int = 10,
        statusFilter: String? = "aprovado",
    ): List<VecSearchResult> {
        val sql = buildString {
            append("""
                SELECT n.id, n.title, n.summary, n.content, n.status, v.distance
                FROM (SELECT rowid, distance FROM vec_knowledge_nodes
                      WHERE embedding MATCH ? AND k = ?) v
                JOIN knowledge_nodes n ON n.rowid = v.rowid
            """.trimIndent())
            if (statusFilter != null) append(" WHERE n.status = ?")
            append(" ORDER BY v.distance ASC")
        }

        val args = mutableListOf<Any>(embeddingBlob, limit)
        if (statusFilter != null) args.add(statusFilter)

        val cursor = db.query(sql, args.toTypedArray())
        return cursor.use { parseResults(it) }
    }

    // ======================== Utilitários ========================

    /**
     * Obtém o rowid interno do SQLite para um knowledge_nodes.id UUID.
     * @return O rowid, ou null se o nó não existir
     */
    fun getRowId(nodeId: String): Long? {
        val cursor = db.query(
            "SELECT rowid FROM knowledge_nodes WHERE id = ?",
            arrayOf(nodeId),
        )
        return cursor.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    /** Contagem total de embeddings no índice. */
    fun count(): Int {
        val cursor = db.query("SELECT COUNT(*) FROM vec_knowledge_nodes")
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    /** Deleta TODOS os embeddings do índice (para rebuild completo). */
    fun clearAll(): Boolean {
        return try {
            db.execSQL("DELETE FROM vec_knowledge_nodes")
            true
        } catch (e: Exception) {
            false
        }
    }

    // ======================== FTS5 ========================

    /**
     * Indexa um nó no FTS5 para busca textual.
     * @param nodeId UUID do knowledge node
     * @param title Título do artigo
     * @param summary Resumo
     * @param content Conteúdo completo
     * @return true se bem-sucedido
     */
    /**
     * Indexa um nó no FTS5 para busca textual.
     *
     * A tabela virtual knowledge_fts foi criada com apenas 5 colunas:
     * node_id, title, summary, content, tags (ver MIGRATION_17_18 passo 6).
     * NÃO inclui created_at/updated_at — FTS5 só aceita as colunas do schema.
     *
     * @param nodeId UUID do knowledge node
     * @param title Título do artigo
     * @param summary Resumo
     * @param content Conteúdo completo
     * @return true se bem-sucedido
     */
    fun indexFts5(nodeId: String, title: String, summary: String, content: String): Boolean {
        return try {
            // Verificar se já existe
            val cursor = db.query(
                "SELECT rowid FROM knowledge_fts WHERE node_id = ?",
                arrayOf(nodeId),
            )
            val exists = cursor.use { it.moveToFirst() }

            if (exists) {
                db.execSQL(
                    "UPDATE knowledge_fts SET title = ?, summary = ?, content = ? WHERE node_id = ?",
                    arrayOf(title, summary, content, nodeId),
                )
            } else {
                db.execSQL(
                    "INSERT INTO knowledge_fts(node_id, title, summary, content) VALUES(?, ?, ?, ?)",
                    arrayOf(nodeId, title, summary, content),
                )
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("VecRepository", "Falha ao indexar FTS5 para nó $nodeId", e)
            false
        }
    }

    /**
     * Remove um nó do índice FTS5.
     */
    fun deleteFts5(nodeId: String): Boolean {
        return try {
            db.execSQL(
                "DELETE FROM knowledge_fts WHERE node_id = ?",
                arrayOf(nodeId),
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Busca textual no FTS5 com BM25 scoring. */
    fun searchFts5(query: String, limit: Int = 20): List<Fts5SearchResult> {
        // Sanitizar query para FTS5: normalizar whitespace sem dropar CJK (中醫, 穴位)
        val sanitized = query.trim().replace(Regex("\\s+"), " ")
        if (sanitized.isBlank()) return emptyList()

        // FTS5 suporta prefix matching com *
        val ftsQuery = sanitized.split(Regex("\\s+")).joinToString(" ") { "$it*" }
        val sql = """
            SELECT f.node_id, n.title, n.summary, f.content,
                   rank_bm25(f) AS score
            FROM knowledge_fts f
            JOIN knowledge_nodes n ON n.id = f.node_id
            WHERE knowledge_fts MATCH ?
            ORDER BY score DESC
            LIMIT ?
        """.trimIndent()

        val cursor = db.query(sql, arrayOf(ftsQuery, limit))
        return cursor.use { parseFts5Results(it) }
    }

    private fun parseFts5Results(cursor: Cursor): List<Fts5SearchResult> {
        val results = mutableListOf<Fts5SearchResult>()
        while (cursor.moveToNext()) {
            results.add(
                Fts5SearchResult(
                    nodeId = cursor.getString(0),
                    title = cursor.getString(1),
                    summary = cursor.getString(2),
                    content = cursor.getString(3),
                    score = cursor.getDouble(4),
                )
            )
        }
        return results
    }

    // ======================== Parsing ========================

    private fun parseResults(cursor: Cursor): List<VecSearchResult> {
        val results = mutableListOf<VecSearchResult>()
        while (cursor.moveToNext()) {
            results.add(
                VecSearchResult(
                    id = cursor.getString(0),
                    title = cursor.getString(1),
                    summary = cursor.getString(2),
                    content = cursor.getString(3),
                    status = cursor.getString(4),
                    distance = cursor.getDouble(5),
                )
            )
        }
        return results
    }

    companion object {
        /**
         * Cria uma instância a partir do AppDatabase.
         * Útil para acesso rápido sem injetar o database inteiro.
         */
        fun from(database: AppDatabase): VecKnowledgeNodeRepository =
            VecKnowledgeNodeRepository(database.openHelper.writableDatabase)
    }
}

/**
 * Resultado de uma busca vetorial no sqlite-vec.
 */
data class VecSearchResult(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val status: String,
    /** Distância coseno: 0 = idêntico, 1 = ortogonal, 2 = oposto */
    val distance: Double,
)

/**
 * Resultado de uma busca textual no FTS5 com BM25 scoring.
 */
data class Fts5SearchResult(
    val nodeId: String,
    val title: String,
    val summary: String,
    val content: String,
    /** BM25 score: quanto maior, melhor */
    val score: Double,
)
