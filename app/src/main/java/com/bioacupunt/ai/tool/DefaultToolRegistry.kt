package com.bioacupunt.ai.tool

import com.bioacupunt.ai.core.AiCapability

class DefaultToolRegistry : ToolRegistry {
    private val tools = mutableMapOf<String, AiTool>()
    override suspend fun allTools(): List<AiTool> = tools.values.toList()
    override suspend fun toolById(id: String): AiTool? = tools[id]
    override suspend fun toolsForCapabilities(capabilities: Set<AiCapability>): List<AiTool> =
        tools.values.filter { tool -> capabilities.any { tool.supportedCapabilities.contains(it) } }
    override suspend fun register(tool: AiTool): Boolean {
        tools[tool.id] = tool
        return true
    }
}
