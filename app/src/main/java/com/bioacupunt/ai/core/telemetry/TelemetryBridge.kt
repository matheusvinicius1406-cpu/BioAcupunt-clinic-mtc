package com.bioacupunt.ai.core.telemetry

import com.bioacupunt.ai.core.AiTelemetry
import com.bioacupunt.ai.core.AiTelemetryEvent
import com.bioacupunt.ai.core.telemetry.AiTelemetrySink

class TelemetryBridge(
    private val sinks: List<AiTelemetrySink> = emptyList()
) : AiTelemetry {
    override suspend fun record(event: AiTelemetryEvent) {
        sinks.forEach { it.record(event) }
    }
}
