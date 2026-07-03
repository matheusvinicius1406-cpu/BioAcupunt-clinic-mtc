package com.bioacupunt.ai.tool

import com.bioacupunt.ai.core.AiCapability

interface ToolRegistry {
    suspend fun allTools(): List<AiTool>
    suspend fun toolById(id: String): AiTool?
    suspend fun toolsForCapabilities(capabilities: Set<AiCapability>): List<AiTool>
    suspend fun register(tool: AiTool): Boolean
}
