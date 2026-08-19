package com.bioacupunt.clinic.data.stt

import com.bioacupunt.clinic.domain.model.SpeechToTextEngine
import com.bioacupunt.clinic.domain.model.SttProvider
import com.bioacupunt.clinic.domain.model.SttState

/**
 * Fake STT engine for testing and environments without real speech recognition.
 * Returns pre-configured transcripts.
 */
class FakeSpeechToTextEngine(
    private val fakeTranscript: String = "",
    private val failOnStart: Boolean = false,
) : SpeechToTextEngine {

    private var currentState: SttState = SttState.IDLE
    private var currentTranscript: String = ""

    override suspend fun start() {
        if (failOnStart) {
            currentState = SttState.ERROR
            return
        }
        currentState = SttState.RECORDING
    }

    override suspend fun stop() {
        currentTranscript = fakeTranscript
        currentState = if (fakeTranscript.isNotBlank()) SttState.READY else SttState.IDLE
    }

    override fun cancel() {
        currentState = SttState.IDLE
        currentTranscript = ""
    }

    override fun transcript(): String = currentTranscript

    override fun state(): SttState = currentState

    override fun provider(): SttProvider = SttProvider.LOCAL
}
