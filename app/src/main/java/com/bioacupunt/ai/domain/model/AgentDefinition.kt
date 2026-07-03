package com.bioacupunt.ai.domain.model

// Centralized AI domain models
sealed interface AiProviderType {
    data object Gemini : AiProviderType
    data object Ollama : AiProviderType
    data object LlamaCpp : AiProviderType
    data object Onnx : AiProviderType
}

data class AiCapabilities(
    val providerType: AiProviderType,
    val supportsStream: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsEmbeddings: Boolean = false,
    val maxContextTokens: Int = 8192,
    val isLocal: Boolean = false
)

data class AiRequest(
    val prompt: String,
    val systemPrompt: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val context: Map<String, String> = emptyMap(),
    val capabilitiesHint: AiCapabilities? = null,
    val preferLocal: Boolean = false
)

data class AiResult(
    val text: String,
    val providerUsed: AiProviderType,
    val modelUsed: String,
    val tokensUsed: Int? = null,
    val latencyMs: Long? = null,
    val isCached: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

sealed interface AiError {
    data class Provider(val providerType: AiProviderType, val cause: Throwable? = null) : AiError
    data object NoProviderAvailable : AiError
    data class LocalModelNotFound(val expected: String) : AiError
    data class InvalidResponse(val reason: String) : AiError
    data class QuotaExceeded(val providerType: AiProviderType) : AiError
    data class Network(val cause: Throwable? = null) : AiError
}

data class AgentDefinition(
    val id: String,
    val name: String,
    val objective: String,
    val systemPrompt: String,
    val tools: List<String> = emptyList(),
    val memoryKey: String? = null,
    val limits: AgentLimits = AgentLimits(),
    val allowedDelegations: List<String> = emptyList()
)

data class AgentLimits(
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
    val maxContextMessages: Int = 20,
    val timeoutMs: Long = 30_000L,
    val allowExternalNetwork: Boolean = false
)

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
