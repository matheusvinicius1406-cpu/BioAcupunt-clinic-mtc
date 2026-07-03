package com.bioacupunt.ai.core

interface AiTelemetry {
    suspend fun record(event: AiTelemetryEvent)
}
