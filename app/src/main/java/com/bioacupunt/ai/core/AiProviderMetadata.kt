package com.bioacupunt.ai.core

data class AiProviderMetadata(
    val providerType: String,
    val executionType: AiExecutionType,
    val pricingModel: AiPricingModel = AiPricingModel.Unknown,
    val estimatedCostPer1kTokens: Double = 0.0,
    val supportsOffline: Boolean = false,
    val supportsStreaming: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsFunctionCalling: Boolean = false,
    val supportsToolCalling: Boolean = false,
    val supportsEmbeddings: Boolean = false,
    val supportsReasoning: Boolean = false,
    val supportsLongContext: Boolean = false,
    val supportsStructuredOutput: Boolean = false,
    val supportsMultimodal: Boolean = false,
    val maxContextWindow: Int = 0,
    val maxOutputTokens: Int = 0,
    val recommendedDeviceClass: DevicePreference = DevicePreference.Auto,
    val minimumAndroidVersion: Int = 0,
    val hardwareAcceleration: String = ""
)
