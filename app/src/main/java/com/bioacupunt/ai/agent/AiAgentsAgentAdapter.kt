package com.bioacupunt.ai.agent

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.ecosystem.AiAgents
import com.bioacupunt.ai.tool.AiTool

class AiAgentsAgentAdapter(
    override val id: String,
    private val toolResolver: suspend (String, Map<String, String>) -> Result<String>
) : Agent {
    override val displayName: String = AiAgents::class.simpleName.orEmpty()
    override val capabilities: Set<AiCapability> = setOf(AiCapability.Chat)
    override val tools: List<AiTool> = emptyList()

    override suspend fun process(request: AiRequest, context: AgentContext): Result<AiResult> {
        val result = context.runTool(id, request.context)
        return result.map { text ->
            AiResult(
                text = text,
                providerId = id,
                modelId = "legacy-agent"
            )
        }
    }
}
