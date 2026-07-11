package com.bioacupunt.ai.orchestrator

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiCostConstraint
import com.bioacupunt.ai.core.AiError
import com.bioacupunt.ai.core.AiLatencyConstraint
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiPrivacyRestriction
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.core.AiTelemetryEvent
import com.bioacupunt.ai.core.rules.AiRoutingRules
import com.bioacupunt.ai.core.scoring.AiProviderScore
import com.bioacupunt.ai.health.HealthRegistry
import com.bioacupunt.ai.health.ProviderStatus
import com.bioacupunt.ai.registry.ProviderRegistry

class ScoredAiOrchestrator(
    private val providers: ProviderRegistry,
    private val healthRegistry: HealthRegistry? = null,
    private val ruleProvider: (suspend (AiRequest) -> AiRoutingRules)? = null,
    private val scoreProvider: (suspend (AiRequest, List<AiProviderScore>) -> List<AiProviderScore>)? = null,
    private val telemetry: (suspend (AiTelemetryEvent) -> Unit)? = null
) : AiOrchestrator {
    override suspend fun execute(request: AiRequest): Result<AiResult> {
        val candidateModels = resolveCandidates(request)
        if (candidateModels.isEmpty()) return Result.failure(com.bioacupunt.ai.core.AiException(AiError.NoProviderAvailable))

        val providersScores = scoreCandidateModels(request, candidateModels)
        val selected = providersScores.firstOrNull() ?: candidateModels.first().let {
            AiProviderScore(providerId = it.providerId, modelId = it.id, score = 0.0)
        }

        val provider = providers.providerById(selected.providerId)
            ?: return Result.failure(com.bioacupunt.ai.core.AiException(AiError.NoProviderAvailable))

        val started = System.currentTimeMillis()
        val primaryResult = provider.generate(request.copy(modelId = selected.modelId))

        val primarySuccess = primaryResult.getOrNull()
        if (primarySuccess != null) {
            telemetry?.invoke(
                AiTelemetryEvent(
                    providerId = provider.id,
                    modelId = selected.modelId,
                    capabilitiesUsed = primarySuccess.capabilitiesUsed,
                    latencyMs = System.currentTimeMillis() - started,
                    success = true,
                    decisionReason = selected.reasons.firstOrNull()
                )
            )
            return primaryResult
        }

        val error = primaryResult.exceptionOrNull() ?: com.bioacupunt.ai.core.AiException(AiError.InvalidResponse("empty"))
        telemetry?.invoke(
            AiTelemetryEvent(
                providerId = provider.id,
                modelId = selected.modelId,
                capabilitiesUsed = emptySet(),
                latencyMs = System.currentTimeMillis() - started,
                success = false,
                errorType = error::class.simpleName,
                decisionReason = selected.reasons.firstOrNull()
            )
        )

        val fallback = providers.allProviders().firstOrNull { it.id != provider.id } ?: return primaryResult
        val fbStarted = System.currentTimeMillis()
        val fbResult = fallback.generate(request)
        val fbSuccess = fbResult.getOrNull()
        if (fbSuccess != null) {
            telemetry?.invoke(
                AiTelemetryEvent(
                    providerId = fallback.id,
                    modelId = fbSuccess.modelId,
                    capabilitiesUsed = fbSuccess.capabilitiesUsed,
                    latencyMs = System.currentTimeMillis() - fbStarted,
                    success = true,
                    fallbackUsed = true,
                    decisionReason = "primary_failed"
                )
            )
        }
        return fbResult
    }

    private suspend fun resolveCandidates(request: AiRequest): List<AiModelDescriptor> {
        val candidates = providers.allProviders().flatMap { it.models }
        val required = request.requiredCapabilities
        val rules = ruleProvider?.invoke(request) ?: AiRoutingRules()
        val health = healthRegistry
        val matches = candidates.filter { model ->
            val provider = providers.providerById(model.providerId)
            val metadata = provider?.metadata
            val healthModel = health?.get(model.providerId, model.id)
            val meetsCapabilities = required.isEmpty() || model.capabilities.containsAll(required)
            val meetsConstraints = meetsCapabilities &&
                !rules.blockedProviders.contains(model.providerId) &&
                !rules.blockedModels.contains(model.id) &&
                meetsHealthStatus(healthModel) &&
                meetsPrivacy(model, request.privacyRestriction, metadata) &&
                meetsCost(model, request.costConstraint, metadata) &&
                meetsLatency(model, request.latencyConstraint, healthModel) &&
                meetsContext(model, rules.maxContextTokens)
            meetsConstraints
        }
        return matches.ifEmpty { candidates }
    }

    private fun meetsHealthStatus(healthModel: com.bioacupunt.ai.health.ProviderHealth?): Boolean {
        if (healthModel == null) return true
        return healthModel.status != ProviderStatus.Unavailable
    }

    private suspend fun scoreCandidateModels(request: AiRequest, candidates: List<AiModelDescriptor>): List<AiProviderScore> {
        val input = candidates.map { model ->
            val healthModel = healthRegistry?.get(model.providerId, model.id)
            val base = model.fallbackOrder.toDouble() + model.contextTokens / 1024.0
            val healthBonus = when (healthModel?.status) {
                com.bioacupunt.ai.health.ProviderStatus.Healthy -> 40.0
                com.bioacupunt.ai.health.ProviderStatus.Degraded -> 10.0
                else -> 0.0
            }
            val latencyPenalty = (healthModel?.avgLatencyMs ?: 0L).coerceAtLeast(0L) / 10.0
            AiProviderScore(
                providerId = model.providerId,
                modelId = model.id,
                score = base + healthBonus - latencyPenalty,
                estimatedLatencyMs = healthModel?.avgLatencyMs,
                reasons = buildList {
                    add("fallback_order=${model.fallbackOrder}")
                    add("ctx=${model.contextTokens}")
                    healthModel?.status?.let { add("health=$it") }
                }
            )
        }.toMutableList()

        val custom = scoreProvider?.invoke(request, input) ?: input
        return custom.sortedByDescending { it.score }
    }

    private fun meetsPrivacy(model: AiModelDescriptor, restriction: AiPrivacyRestriction, metadata: com.bioacupunt.ai.core.AiProviderMetadata?): Boolean {
        if (restriction == AiPrivacyRestriction.None) return true
        return metadata?.supportsOffline == true
    }

    private fun meetsCost(model: AiModelDescriptor, constraint: AiCostConstraint?, metadata: com.bioacupunt.ai.core.AiProviderMetadata?): Boolean {
        if (constraint == null) return true
        val max = constraint.maxCostPer1kTokens ?: return true
        val cost = metadata?.estimatedCostPer1kTokens ?: 0.0
        return cost <= max
    }

    private fun meetsLatency(model: AiModelDescriptor, constraint: AiLatencyConstraint?, healthModel: com.bioacupunt.ai.health.ProviderHealth?): Boolean {
        if (constraint == null) return true
        val observed = healthModel?.avgLatencyMs ?: return true
        val max = constraint.maxResponseTimeMs ?: return true
        return observed <= max
    }

    private fun meetsContext(model: AiModelDescriptor, maxContextTokens: Int?): Boolean {
        if (maxContextTokens == null) return true
        return model.contextTokens >= maxContextTokens
    }
}
