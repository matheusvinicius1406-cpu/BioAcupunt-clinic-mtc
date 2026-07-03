package com.bioacupunt.ai.core

data class AiModelDescriptor(
    val id: String,
    val providerId: String,
    val displayName: String,
    val capabilities: Set<AiCapability> = emptySet(),
    val contextTokens: Int = 8192,
    val isLocal: Boolean = false,
    val fallbackOrder: Int = 100,
    val metadata: AiProviderMetadata = AiProviderMetadata(providerType = providerId, executionType = AiExecutionType.Remote)
)
