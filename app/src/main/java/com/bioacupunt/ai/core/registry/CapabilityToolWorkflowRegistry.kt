package com.bioacupunt.ai.core.registry

import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiProvider

interface CapabilityRegistry {
    suspend fun allCapabilities(): Set<AiCapability>
}

interface ToolRegistry {
    suspend fun register(tool: Any): Boolean
}

interface WorkflowRegistry {
    suspend fun register(workflow: Any): Boolean
}
