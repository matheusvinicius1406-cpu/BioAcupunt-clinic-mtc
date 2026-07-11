package com.bioacupunt.ai.tool

import com.bioacupunt.ai.core.AiCapability

interface AiTool {
    val id: String
    val displayName: String
    val supportedCapabilities: Set<AiCapability> get() = emptySet()
    suspend fun execute(input: Map<String, String>): Result<String>
}
