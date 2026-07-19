package com.bioacupunt.ai.registry

import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiCapability

/**
 * @param initial provedores pré-registrados de forma **não suspensa**. Existe para o
 * wiring do [com.bioacupunt.di.AppContainer] montar o registro sem `runBlocking` na
 * thread de UI — registrar é só um put de mapa; forçar uma corrotina para isso trancava
 * a composição na primeira navegação até a Biblioteca/Assistente.
 */
class SimpleProviderRegistry(initial: List<AiProvider> = emptyList()) : ProviderRegistry {
    private val providers = mutableMapOf<String, AiProvider>().apply {
        initial.forEach { put(it.id, it) }
    }
    override suspend fun allProviders(): List<AiProvider> = providers.values.toList()
    override suspend fun providerById(id: String): AiProvider? = providers[id]
    override suspend fun register(provider: AiProvider): Boolean {
        providers[provider.id] = provider
        return true
    }
    override suspend fun availableProviders(): List<AiProvider> = providers.values.filter { runCatching { it.isAvailable() }.getOrDefault(false) }
    override suspend fun providersForCapabilities(required: Set<AiCapability>): List<AiProvider> =
        availableProviders().filter { p -> required.all { req -> p.capabilities.capabilities.contains(req) } }
}
