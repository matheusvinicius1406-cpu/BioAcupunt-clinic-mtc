package com.bioacupunt.ai.domain.model

data class RetrievalChunk(
    val id: String,
    val text: String,
    val source: String,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)

data class RagContext(
    val chunks: List<RetrievalChunk> = emptyList(),
    val query: String = "",
    val topK: Int = 5,
    val minScore: Double = 0.3
)

data class KnowledgeEntry(
    val id: String,
    val title: String,
    val text: String,
    val tags: List<String> = emptyList(),
    val source: String? = null,
    val embedding: FloatArray? = null
)
