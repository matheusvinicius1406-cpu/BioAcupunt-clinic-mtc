package com.bioacupunt.ai.agent

import com.bioacupunt.ai.core.AiCapability

interface AgentRegistry {
    suspend fun allAgents(): List<Agent>
    suspend fun agentById(id: String): Agent?
    suspend fun agentsForCapabilities(capabilities: Set<AiCapability>): List<Agent>
    suspend fun register(agent: Agent): Boolean
}
