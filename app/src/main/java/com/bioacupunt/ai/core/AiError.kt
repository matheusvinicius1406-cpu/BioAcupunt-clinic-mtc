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

/** Shown to the practitioner: says what to do, not just what broke. */
fun AiError.userMessage(): String = when (this) {
    is AiError.NoProviderAvailable ->
        "IA não disponível. Abra Ajustes > IA e baixe o modelo local para usar offline no aparelho."
    is AiError.LocalModelNotFound ->
        "Modelo local ainda não baixado. Abra Ajustes > IA para baixá-lo (roda offline no aparelho)."
    is AiError.QuotaExceeded ->
        "A cota da API ($providerType) acabou. Verifique o plano da sua chave em Ajustes > IA."
    is AiError.Network -> "Sem conexão com o serviço de IA. Verifique a internet e tente de novo."
    is AiError.Security -> "Requisição bloqueada por segurança: $reason"
    is AiError.CapabilityNotSupported -> "O modelo configurado não suporta este pedido ($required)."
    is AiError.InvalidResponse -> "A IA devolveu resposta inválida ($reason). Tente de novo."
    is AiError.Provider ->
        cause?.message?.takeIf { it.isNotBlank() }?.let { "Falha no provedor $providerType: $it" }
            ?: "Falha no provedor $providerType."
}

class AiException(val error: AiError) : Exception(error.userMessage())
