package com.bioacupunt.ai.core

data class AiProviderCapabilities(
    val capabilities: Set<AiCapability> = emptySet(),
    val maxContextTokens: Int = 8192,
    val supportsStream: Boolean = false,
    val supportsLocalExecution: Boolean = false,
    val preferredDevice: DevicePreference = DevicePreference.Auto
)

enum class DevicePreference { CPU, GPU, NPU, Auto }
