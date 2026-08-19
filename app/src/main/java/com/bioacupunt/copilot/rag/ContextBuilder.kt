package com.bioacupunt.copilot.rag

import com.bioacupunt.copilot.retrieval.RetrievalHit
import com.bioacupunt.copilot.retrieval.UnifiedRetrievalResult

/**
 * §18-20 CONTEXT BUILDER
 *
 * Builds structured context for the LLM from retrieval results.
 * Each context item contains: entity, content, source, citation, evidence, relations, provenance.
 *
 * Context budget:
 * - maxDocuments: 20
 * - maxCharacters: 8000
 * - maxTokens (estimated): 2000
 * - maxGraphNodes: 30
 * - maxEvidenceItems: 50
 *
 * Prioritization (§20):
 * 1. Direct evidence
 * 2. Direct entity matches
 * 3. High-confidence graph paths
 * 4. Strong citations
 * 5. Related context
 * 6. Generic background
 */
class ContextBuilder(
    private val config: ContextBudget = ContextBudget(),
) {

    data class ContextBudget(
        val maxDocuments: Int = 20,
        val maxCharacters: Int = 8000,
        val maxTokens: Int = 2000,
        val maxGraphNodes: Int = 30,
        val maxEvidenceItems: Int = 50,
    )

    data class StructuredContext(
        val items: List<ContextItem>,
        val totalCharacters: Int,
        val totalTokens: Int,
        val truncated: Boolean,
        val evidenceIds: List<String>,
    )

    data class ContextItem(
        val entity: String,
        val content: String,
        val source: String? = null,
        val citation: String? = null,
        val evidence: List<String> = emptyList(),
        val relations: List<String> = emptyList(),
        val provenance: String? = null,
        val knowledgeVersion: String? = null,
        val priority: Int = 0, // lower = higher priority
        val sourceType: String = "unknown",
    )

    /**
     * Build structured context from retrieval results.
     * Respects budget limits and prioritization policy.
     */
    fun build(
        retrievalResult: UnifiedRetrievalResult,
        query: String,
        clinicalContext: String? = null,
        patientContext: String? = null,
    ): StructuredContext {
        val items = mutableListOf<ContextItem>()
        var totalChars = 0
        var totalTokens = 0
        val usedEvidence = mutableListOf<String>()

        // Prioritize and build context items
        val prioritized = retrievalResult.results
            .sortedBy { calculatePriority(it) }

        for (hit in prioritized) {
            if (items.size >= config.maxDocuments) break
            if (totalChars >= config.maxCharacters) break
            if (totalTokens >= config.maxTokens) break

            val content = hit.content.take(config.maxCharacters - totalChars)
            if (content.isBlank()) continue

            val estimatedTokens = (content.length / 4).toInt() // rough estimate
            if (totalTokens + estimatedTokens > config.maxTokens) break

            items.add(
                ContextItem(
                    entity = hit.entity?.canonicalName ?: hit.entityId,
                    content = content,
                    source = hit.entity?.sourceIds?.firstOrNull(),
                    citation = hit.entity?.let { "${it.canonicalName} (${it.type})" },
                    evidence = hit.evidenceIds.take(config.maxEvidenceItems - usedEvidence.size),
                    relations = hit.metadata["relations"]?.split(",") ?: emptyList(),
                    provenance = hit.provenance,
                    knowledgeVersion = hit.knowledgeVersion,
                    priority = calculatePriority(hit),
                    sourceType = hit.sourceType.name,
                )
            )

            totalChars += content.length
            totalTokens += estimatedTokens
            usedEvidence.addAll(hit.evidenceIds)
        }

        return StructuredContext(
            items = items,
            totalCharacters = totalChars,
            totalTokens = totalTokens,
            truncated = retrievalResult.results.size > items.size ||
                totalChars < retrievalResult.results.sumOf { it.content.length },
            evidenceIds = usedEvidence.distinct(),
        )
    }

    /**
     * Calculate priority for a retrieval hit.
     * Lower number = higher priority (used for sorting).
     */
    private fun calculatePriority(hit: RetrievalHit): Int {
        var priority = 5 // default: related context

        // Direct evidence (highest priority)
        if (hit.evidenceIds.isNotEmpty()) priority = minOf(priority, 1)

        // Direct entity matches
        if (hit.entity != null && hit.normalizedScore > 0.8) priority = minOf(priority, 2)

        // High-confidence graph paths
        if (hit.sourceType == com.bioacupunt.copilot.retrieval.RetrievalSource.GRAPH &&
            hit.graphDepth <= 1
        ) {
            priority = minOf(priority, 3)
        }

        // Strong citations
        if (hit.provenance != null) priority = minOf(priority, 4)

        return priority
    }

    /**
     * Format context as a prompt-ready string for the LLM.
     */
    fun formatForPrompt(context: StructuredContext): String = buildString {
        appendLine("=== CONTEXTO CLÍNICO ===")
        appendLine()

        for ((index, item) in context.items.withIndex()) {
            appendLine("--- Item ${index + 1} [${item.sourceType}] ---")
            appendLine("Entidade: ${item.entity}")
            appendLine("Conteúdo: ${item.content}")
            if (item.citation != null) appendLine("Citação: ${item.citation}")
            if (item.evidence.isNotEmpty()) appendLine("Evidências: ${item.evidence.joinToString(", ")}")
            if (item.provenance != null) appendLine("Proveniência: ${item.provenance}")
            appendLine()
        }

        if (context.truncated) {
            appendLine("⚠️ Contexto truncado — nem todos os resultados couberam no orçamento.")
        }

        appendLine("Total: ${context.items.size} itens, ${context.totalCharacters} caracteres, ~${context.totalTokens} tokens")
    }
}
