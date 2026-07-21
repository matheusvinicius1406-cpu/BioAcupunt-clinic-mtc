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

    data class ValidationError(val message: String) :
        AppError("ERR_VAL_001", message, retryable = false)

    data class Unknown(val cause: Throwable? = null) :
        AppError("ERR_UNK_001", "Erro inesperado.", retryable = false)

    /**
     * A write was rejected because it referenced something that no longer
     * exists, or duplicated something unique — a data-shape problem, not a
     * storage problem.
     *
     * This exists because it was missing. Every repository used to funnel every
     * exception into [DatabaseError] ("Falha ao acessar dados locais"), which
     * described a disk failure. The real cause was a foreign key pointing at the
     * wrong table, and for as long as the message claimed the disk was at fault,
     * nobody looked at the schema. An error message that misdescribes its own
     * cause does not just fail to help — it actively sends you the wrong way.
     */
    data class ConstraintError(val cause: Throwable? = null) :
        AppError(
            "ERR_DB_002",
            "Não foi possível salvar: o registro faz referência a um paciente que não existe mais.",
            retryable = false,
        )

    companion object {
        /**
         * Classifies a persistence exception.
         *
         * Matched by class name rather than by type so this file stays pure
         * Kotlin with no Android imports — the same reason the safety engine is
         * pure Kotlin: it has to be readable and testable on the JVM, without a
         * device in the loop.
         *
         * Exception messages are never forwarded to the user. They are written
         * by developers for developers ("Tenant mismatch on appointment
         * operation: 0 != 1") and mean nothing to a doctor mid-consultation.
         * Log [cause]; show the fixed text.
         */
        fun from(cause: Throwable): AppError {
            val names = generateSequence(cause) { it.cause }
                .map { it.javaClass.name }
                .toList()
            return when {
                names.any { it.endsWith("SQLiteConstraintException") } -> ConstraintError(cause)
                cause is IllegalArgumentException || cause is IllegalStateException ->
                    ValidationError("Não foi possível salvar: dados inválidos para este registro.")
                names.any { it.startsWith("java.io.") } -> NetworkError(cause)
                else -> DatabaseError(cause)
            }
        }
    }
}
