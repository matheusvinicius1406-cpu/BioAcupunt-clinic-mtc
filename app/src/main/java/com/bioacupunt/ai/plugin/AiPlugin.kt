package com.bioacupunt.ai.plugin

import com.bioacupunt.ai.core.AiProvider

interface AiPlugin {
    val id: String
    val displayName: String
    suspend fun register(provider: AiProvider): Boolean
}
