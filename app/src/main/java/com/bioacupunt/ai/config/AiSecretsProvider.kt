package com.bioacupunt.ai.config

interface AiSecretsProvider {
    suspend fun apiKeyFor(providerId: String): String?
    suspend fun setApiKey(providerId: String, key: String)
}
