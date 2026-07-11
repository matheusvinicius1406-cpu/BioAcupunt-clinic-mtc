package com.bioacupunt.ai.agent

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.tool.DefaultToolRegistry
import com.bioacupunt.ai.tool.ToolRegistry

class DefaultAgentRegistry(
    private val toolRegistry: ToolRegistry = DefaultToolRegistry()
) : AgentRegistry {
    private val agents = mutableMapOf<String, Agent>()
    override suspend fun allAgents(): List<Agent> = agents.values.toList()
    override suspend fun agentById(id: String): Agent? = agents[id]
    override suspend fun agentsForCapabilities(capabilities: Set<AiCapability>): List<Agent> =
        agents.values.filter { it.capabilities.any { capabilities.contains(it) } }
    override suspend fun register(agent: Agent): Boolean {
        agents[agent.id] = agent
        return true
    }
    suspend fun runAgent(agentId: String, request: AiRequest, context: AgentContext): Result<AiResult> =
        agentById(agentId)?.process(request, context) ?: Result.failure(IllegalArgumentException("Agent not found: $agentId"))
}
