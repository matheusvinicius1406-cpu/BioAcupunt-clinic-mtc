package com.example.ai

import com.example.core.DomainEvent
import com.example.core.EventBus
import java.util.UUID

// ---------------- PROVIDERS & MANAGERS ----------------

enum class AiProviderType {
    GEMINI, OPENAI, CLAUDE, DEEPSEEK, LOCAL_ON_DEVICE
}

data class AiRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val temperature: Float = 0.2f
)

data class AiResponse(
    val content: String,
    val provider: AiProviderType,
    val usageTokens: Int
)

interface AiProvider {
    val type: AiProviderType
    suspend fun generateContent(request: AiRequest): AiResponse
}

class ProviderManager(private val providers: List<AiProvider>) {
    fun getProvider(type: AiProviderType): AiProvider {
        return providers.find { it.type == type }
            ?: throw IllegalArgumentException("No provider registered for type: $type")
    }
}

// Concrete Gemini Mock Provider implementing the real abstract interface
class GeminiProvider : AiProvider {
    override val type = AiProviderType.GEMINI
    override suspend fun generateContent(request: AiRequest): AiResponse {
        // Concrete integration call simulation (easily swappable with real Google GenAI Client)
        return AiResponse(
            content = "Resultado de inferência clínica via Google Gemini Pro: Padrão diagnosticado como Deficiência de Yin do Rim. Sugere-se pontos R3, R6, BP6 com retenção por 20min.",
            provider = AiProviderType.GEMINI,
            usageTokens = 120
        )
    }
}

// ---------------- RAG ENGINE ----------------

data class VectorDocument(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val embedding: List<Float>
)

interface EmbeddingService {
    suspend fun generateEmbedding(text: String): List<Float>
}

class SimpleEmbeddingService : EmbeddingService {
    override suspend fun generateEmbedding(text: String): List<Float> {
        // Simulated embedding vector generation (128-dim mock vector)
        return List(128) { 0.1f * text.length }
    }
}

class VectorStore {
    private val store = mutableListOf<VectorDocument>()

    fun addDocument(doc: VectorDocument) {
        store.add(doc)
    }

    fun findNearest(embedding: List<Float>, topK: Int = 3): List<VectorDocument> {
        return store.map { doc ->
            val distance = cosineSimilarity(doc.embedding, embedding)
            Pair(doc, distance)
        }.sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Double {
        if (v1.size != v2.size) return 0.0
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
    }
}

class RagEngine(
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore
) {
    suspend fun retrieveRelevantContext(query: String): String {
        val queryEmbedding = embeddingService.generateEmbedding(query)
        val nearestDocs = vectorStore.findNearest(queryEmbedding, topK = 2)
        return nearestDocs.joinToString("\n---\n") { it.content }
    }
}

// ---------------- SUB-AGENTS KERNEL ----------------

sealed interface ClinicalAgent {
    val agentName: String
    suspend fun process(input: String): String
}

class DiagnosisAgent(private val provider: AiProvider, private val rag: RagEngine) : ClinicalAgent {
    override val agentName = "Clinical Diagnosis Sub-Agent"
    override suspend fun process(input: String): String {
        val context = rag.retrieveRelevantContext(input)
        val prompt = "Contexto Clinico:\n$context\n\nQueixa do Paciente: $input\nForneça uma hipótese de padrão MTC."
        val response = provider.generateContent(AiRequest(prompt))
        return response.content
    }
}

class ProtocolAgent(private val provider: AiProvider) : ClinicalAgent {
    override val agentName = "Protocol Automation Sub-Agent"
    override suspend fun process(input: String): String {
        val prompt = "Dado o diagnóstico '$input', formule um plano de acupontos fundamentais e fórmulas fitoterápicas correlatas."
        return provider.generateContent(AiRequest(prompt)).content
    }
}

class ResearchAgent(private val provider: AiProvider, private val rag: RagEngine) : ClinicalAgent {
    override val agentName = "Academic Research Sub-Agent"
    override suspend fun process(input: String): String {
        val context = rag.retrieveRelevantContext(input)
        val prompt = "Busque embasamento acadêmico recente para a acupuntura aplicada a: $input\nEvidências:\n$context"
        return provider.generateContent(AiRequest(prompt)).content
    }
}

// AI Interaction Entity for Logs
data class AIInteractionLog(
    val id: String = UUID.randomUUID().toString(),
    val agentName: String,
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)
