package com.bioacupunt.ai.plugin

import com.bioacupunt.ai.core.AiProvider

class DefaultPluginManager(
    private val plugins: List<AiPlugin>
) {
    suspend fun registerAll(providerRegistry: com.bioacupunt.ai.registry.ProviderRegistry): Boolean {
        var ok = true
        plugins.forEach { plugin ->
            ok = ok && runCatching { plugin.register(providerRegistry.allProviders().first()) }.isSuccess
        }
        return ok
    }
}
