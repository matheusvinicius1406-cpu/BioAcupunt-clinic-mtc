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

        // Walk the remaining candidates in scored order, not just the single
        // next provider in registration order.
        //
        // The previous version took `firstOrNull { it.id != provider.id }` and
        // stopped there. With three providers registered, a failure of the first
        // two meant the third was never tried at all — and the error the doctor
        // saw came from whichever one happened to be second, which tells her
        // nothing about why the assistant is unavailable.
        //
        // Only providers that passed `resolveCandidates` are eligible here: a
        // provider excluded on privacy or availability grounds must not be
        // reachable through the failure path either.
        val remaining = providersScores
            .map { it.providerId }
            .distinct()
            .filter { it != provider.id }
            .mapNotNull { providers.providerById(it) }

        var lastResult = primaryResult
        for (candidate in remaining) {
            val fbStarted = System.currentTimeMillis()
            val fbResult = candidate.generate(request)
            val fbSuccess = fbResult.getOrNull()
            if (fbSuccess != null) {
                telemetry?.invoke(
                    AiTelemetryEvent(
                        providerId = candidate.id,
                        modelId = fbSuccess.modelId,
                        capabilitiesUsed = fbSuccess.capabilitiesUsed,
                        latencyMs = System.currentTimeMillis() - fbStarted,
                        success = true,
                        fallbackUsed = true,
                        decisionReason = "primary_failed"
                    )
                )
                return fbResult
            }
            lastResult = fbResult
        }
        return lastResult
    }

    private suspend fun resolveCandidates(request: AiRequest): List<AiModelDescriptor> {
        val candidates = providers.allProviders().flatMap { it.models }
        val required = request.requiredCapabilities
        val rules = ruleProvider?.invoke(request) ?: AiRoutingRules()
        val health = healthRegistry

        // Ask each provider whether it can actually serve a request right now.
        //
        // This check was missing entirely, and it is why the assistant was dead
        // on every device: the on-device model declares fallbackOrder = 0 so it
        // always won the scoring below, and it was routed to even though its
        // model file had never been downloaded. It failed with "Modelo local
        // ausente", and the doctor saw that instead of an answer.
        //
        // `isAvailable()` has existed on AiProvider all along and is correctly
        // implemented by LocalLlmProvider — nothing in the routing path had ever
        // called it. Evaluated once per provider rather than once per model,
        // since it can do real work (a file-existence check today, potentially a
        // network probe later).
        val availability = providers.allProviders().associate { provider ->
            provider.id to runCatching { provider.isAvailable() }.getOrDefault(false)
        }

        val matches = candidates.filter { model ->
            val provider = providers.providerById(model.providerId)
            val metadata = provider?.metadata
            val healthModel = health?.get(model.providerId, model.id)
            val meetsCapabilities = required.isEmpty() || model.capabilities.containsAll(required)
            val meetsConstraints = meetsCapabilities &&
                availability[model.providerId] == true &&
                !rules.blockedProviders.contains(model.providerId) &&
                !rules.blockedModels.contains(model.id) &&
                meetsHealthStatus(healthModel) &&
                meetsPrivacy(model, request.privacyRestriction, metadata) &&
                meetsCost(model, request.costConstraint, metadata) &&
                meetsLatency(model, request.latencyConstraint, healthModel) &&
                meetsContext(model, rules.maxContextTokens)
            meetsConstraints
        }

        // No `ifEmpty { candidates }` fallback any more.
        //
        // That fallback is what made the availability check pointless in the
        // first place: filter everything out, then hand back the unfiltered list
        // anyway. It also silently defeats *every* constraint above — a request
        // marked as privacy-restricted would be routed to a cloud provider the
        // moment no local option qualified, sending patient data off the device
        // precisely because the rules said it must not go.
        //
        // An empty list is answered with NoProviderAvailable by the caller,
        // which is the honest outcome: nothing here can serve this request.
        return matches
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
