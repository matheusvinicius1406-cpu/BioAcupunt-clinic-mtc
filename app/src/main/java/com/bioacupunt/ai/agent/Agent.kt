package com.bioacupunt.ai.agent

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.tool.AiTool

interface AgentContext {
    suspend fun delegateTo(agentId: String, request: AiRequest): Result<AiResult>
    suspend fun runTool(toolId: String, input: Map<String, String>): Result<String>
}

interface Agent {
    val id: String
    val displayName: String
    val capabilities: Set<AiCapability>
    val tools: List<AiTool>
    suspend fun process(request: AiRequest, context: AgentContext): Result<AiResult>
}
