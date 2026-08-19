package com.bioacupunt.clinic.domain.model

/**
 * Speech-to-Text engine abstraction.
 *
 * Provider-agnostic: the domain never knows if it's running local or cloud.
 * Priority: LOCAL. Cloud only with explicit configuration and policy.
 *
 * Audio stays local when possible.
 */
interface SpeechToTextEngine {
    suspend fun start()
    suspend fun stop()
    fun cancel()
    fun transcript(): String
    fun state(): SttState
    fun provider(): SttProvider
}

enum class SttState {
    IDLE,
    RECORDING,
    PROCESSING,
    READY,
    ERROR,
    PERMISSION_DENIED,
}

enum class SttProvider {
    LOCAL,
    CLOUD,
}
