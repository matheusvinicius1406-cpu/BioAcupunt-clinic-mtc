package com.bioacupunt.copilot.observability

import com.bioacupunt.copilot.retrieval.IntentType
import com.bioacupunt.copilot.retrieval.UnifiedRetrievalResult

/**
 * §49 OBSERVABILITY
 *
 * Metrics for the copilot pipeline:
 * - intent: which intent was detected
 * - retrievalCount: how many candidates retrieved
 * - retrievalLatencyMs: time for retrieval
 * - rerankLatencyMs: time for reranking
 * - contextSize: characters in context
 * - evidenceCount: evidence items resolved
 * - llmProvider: which model was used
 * - llmLatencyMs: time for LLM generation
 * - validationResult: VALID/HAS_WARNINGS/REJECTED
 * - fallback: whether fallback was used
 *
 * NEVER logs clinical content (patient data, symptoms, diagnoses).
 */
class CopilotMetrics {

    data class MetricSnapshot(
        val intent: IntentType,
        val retrievalCount: Int,
        val retrievalLatencyMs: Long,
        val rerankLatencyMs: Long = 0,
        val contextSize: Int,
        val evidenceCount: Int,
        val llmProvider: String = "local",
        val llmLatencyMs: Long = 0,
        val validationResult: String,
        val fallback: Boolean,
        val totalLatencyMs: Long,
    )

    private val snapshots = mutableListOf<MetricSnapshot>()

    /**
     * Record a metric snapshot.
     * Clinical content is NEVER logged.
     */
    fun record(snapshot: MetricSnapshot) {
        snapshots.add(snapshot)
    }

    /**
     * Get average latency for retrieval.
     */
    fun avgRetrievalLatency(): Double {
        if (snapshots.isEmpty()) return 0.0
        return snapshots.map { it.retrievalLatencyMs }.average()
    }

    /**
     * Get average total latency.
     */
    fun avgTotalLatency(): Double {
        if (snapshots.isEmpty()) return 0.0
        return snapshots.map { it.totalLatencyMs }.average()
    }

    /**
     * Get p95 latency.
     */
    fun p95TotalLatency(): Long {
        if (snapshots.isEmpty()) return 0L
        val sorted = snapshots.map { it.totalLatencyMs }.sorted()
        val index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
        return sorted[index]
    }

    /**
     * Get fallback rate.
     */
    fun fallbackRate(): Double {
        if (snapshots.isEmpty()) return 0.0
        return snapshots.count { it.fallback }.toDouble() / snapshots.size
    }

    /**
     * Clear metrics (for testing).
     */
    fun clear() {
        snapshots.clear()
    }
}
