package com.bioacupunt.ai.data.rag

import com.bioacupunt.ai.domain.model.KnowledgeEntry
import com.bioacupunt.ai.domain.model.RagContext
import com.bioacupunt.ai.domain.model.RetrievalChunk

interface RagStore {
    suspend fun upsert(entries: List<KnowledgeEntry>)
    suspend fun query(context: RagContext): List<RetrievalChunk>
    suspend fun clear()
}
