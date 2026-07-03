package com.bioacupunt.ai.core

sealed interface AiError {
    data class Provider(val providerType: String, val cause: Throwable? = null) : AiError
    data object NoProviderAvailable : AiError
    data class LocalModelNotFound(val expected: String) : AiError
    data class InvalidResponse(val reason: String) : AiError
    data class QuotaExceeded(val providerType: String) : AiError
    data class Network(val cause: Throwable? = null) : AiError
    data class CapabilityNotSupported(val required: String) : AiError
    data class Security(val reason: String) : AiError
}
