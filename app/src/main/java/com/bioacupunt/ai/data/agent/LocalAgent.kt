package com.bioacupunt.ai.data.agent

import com.bioacupunt.ai.domain.usecase.AiAgent
import com.bioacupunt.ai.domain.usecase.AiAgentContext
import com.bioacupunt.ai.domain.usecase.AgentTool
import com.bioacupunt.ai.domain.usecase.AgentDefinition
import com.bioacupunt.ai.domain.usecase.AgentLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAgent(
    override val definition: AgentDefinition
) : AiAgent {

    override suspend fun execute(input: String, context: AiAgentContext): Result<String> =
        withContext(Dispatchers.Default) {
            val system = buildSystemPrompt(context)
            val prompt = buildUserPrompt(input, context)

            val request = AiRequest(
                prompt = prompt,
                systemPrompt = system,
                temperature = definition.limits.temperature,
                maxTokens = definition.limits.maxTokens,
                preferLocal = true
            )

            val orchestrator = AiOrchestrator(
                providers = listOf(
                    FallbackProvider()
                )
            )

            orchestrator.generate(request)
                .map { it.text }
                .onFailure { Result.failure(Exception("Local agent execution failed", it)) }
        }

    private fun buildSystemPrompt(context: AiAgentContext): String =
        buildString {
            appendLine(definition.systemPrompt)
            appendLine()
            appendLine("Context:")
            appendLine("- userId: ${context.userId ?: "unknown"}")
            appendLine("- tenantId: ${context.tenantId ?: "unknown"}")
            if (context.knowledge.isNotEmpty()) {
                appendLine("- knowledge: ${context.knowledge.joinToString("; ")}")
            }
        }.trimIndent()

    private fun buildUserPrompt(input: String, context: AiAgentContext): String =
        buildString {
            appendLine(input)
            if (context.metadata.isNotEmpty()) {
                appendLine()
                appendLine("Metadata:")
                context.metadata.forEach { (k, v) -> appendLine("- $k: $v") }
            }
        }.trimIndent()
}
