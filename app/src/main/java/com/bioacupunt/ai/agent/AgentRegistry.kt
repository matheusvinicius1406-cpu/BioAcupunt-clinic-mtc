package com.bioacupunt.ai.agent

interface AgentRegistry {
    suspend fun allAgents(): List<Agent>
    suspend fun agentById(id: String): Agent?
    suspend fun agentsForCapabilities(capabilities: Set<AiCapability>): List<Agent>
    suspend fun register(agent: Agent): Boolean
}
