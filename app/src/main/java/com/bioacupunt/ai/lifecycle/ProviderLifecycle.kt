package com.bioacupunt.ai.lifecycle

import com.bioacupunt.ai.core.AiProvider

interface ProviderLifecycle {
    suspend fun initialize(provider: AiProvider)
    suspend fun authenticate(provider: AiProvider): Boolean
    suspend fun shutdown(provider: AiProvider)
}
