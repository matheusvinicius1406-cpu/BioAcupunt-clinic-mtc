package com.bioacupunt.ai.core

interface AiProvider {
    val id: String
    val displayName: String
    val capabilities: AiProviderCapabilities
    val metadata: AiProviderMetadata
    val models: List<AiModelDescriptor>

    suspend fun isAvailable(): Boolean
    suspend fun generate(request: AiRequest): Result<AiResult>
    suspend fun capabilitiesForModel(modelId: String): AiProviderCapabilities = capabilities
}
