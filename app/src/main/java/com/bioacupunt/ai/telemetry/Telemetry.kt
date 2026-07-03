package com.bioacupunt.ai.telemetry

import com.bioacupunt.ai.core.AiTelemetry
import com.bioacupunt.ai.core.AiTelemetryEvent

object NoopAiTelemetry : AiTelemetry {
    override suspend fun record(event: AiTelemetryEvent) = Unit
}

class InMemoryAiTelemetry : AiTelemetry {
    private val events = mutableListOf<AiTelemetryEvent>()
    override suspend fun record(event: AiTelemetryEvent) {
        events += event
        println("[TELEMETRY] ${event.providerId} | ${event.modelId} | ${event.success} | ${event.latencyMs}ms")
    }
}
