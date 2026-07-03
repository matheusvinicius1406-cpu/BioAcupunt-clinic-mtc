package com.bioacupunt.ai.domain.usecase

import com.bioacupunt.ai.domain.model.AgentDefinition

interface AgentTool {
    val id: String
    val name: String
    suspend fun execute(context: Map<String, String>): Result<String>
}

interface AiAgent {
    val definition: AgentDefinition
    suspend fun execute(input: String, context: AiAgentContext): Result<String>
}

data class AiAgentContext(
    val userId: String? = null,
    val tenantId: String? = null,
    val knowledge: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
