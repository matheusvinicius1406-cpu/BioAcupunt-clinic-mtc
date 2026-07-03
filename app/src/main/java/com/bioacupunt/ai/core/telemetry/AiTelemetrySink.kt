package com.bioacupunt.ai.core.telemetry

import com.bioacupunt.ai.core.AiTelemetryEvent

interface AiTelemetrySink {
    suspend fun record(event: AiTelemetryEvent)
}
