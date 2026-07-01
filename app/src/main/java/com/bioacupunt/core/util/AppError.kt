package com.bioacupunt.core.util

sealed class AppError(
    val code: String,
    val userMessage: String,
    val retryable: Boolean = false
) {
    data class NetworkError(val cause: Throwable? = null) :
        AppError("ERR_NET_001", "Sem conexão ou serviço indisponível.", retryable = true)

    data class DatabaseError(val cause: Throwable? = null) :
        AppError("ERR_DB_001", "Falha ao acessar dados locais.", retryable = false)

    data class AIError(val cause: Throwable? = null) :
        AppError("ERR_AI_001", "Assistente indisponível no momento.", retryable = true)

    data class AuthError(val cause: Throwable? = null) :
        AppError("ERR_AUTH_001", "Falha de autenticação.", retryable = false)

    data class SyncError(val cause: Throwable? = null) :
        AppError("ERR_SYNC_001", "Sincronização temporariamente indisponível.", retryable = true)

    data class Unknown(val cause: Throwable? = null) :
        AppError("ERR_UNK_001", "Erro inesperado.", retryable = false)
}
