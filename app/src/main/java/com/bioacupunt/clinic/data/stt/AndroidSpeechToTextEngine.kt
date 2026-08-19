package com.bioacupunt.clinic.data.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.bioacupunt.clinic.domain.model.SpeechToTextEngine
import com.bioacupunt.clinic.domain.model.SttProvider
import com.bioacupunt.clinic.domain.model.SttState

/**
 * Android STT implementation using SpeechRecognizer.
 *
 * This is the LOCAL provider. Cloud provider can be added separately.
 * Audio stays on-device.
 *
 * Limitations:
 * - Requires Google Speech services (most Android devices)
 * - Requires RECORD_AUDIO permission
 * - Not available in all emulators
 * - Partial transcripts may not be supported on all devices
 */
class AndroidSpeechToTextEngine(
    private val context: Context,
) : SpeechToTextEngine {

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState: SttState = SttState.IDLE
    private var currentTranscript: String = ""
    private var partialTranscript: String = ""

    override suspend fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            currentState = SttState.ERROR
            return
        }

        currentState = SttState.RECORDING
        currentTranscript = ""
        partialTranscript = ""

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    currentState = SttState.RECORDING
                }

                override fun onBeginningOfSpeech() {
                    currentState = SttState.RECORDING
                }

                override fun onRmsChanged(rmsdB: Float) { /* no-op */ }

                override fun onBufferReceived(buffer: ByteArray?) { /* no-op */ }

                override fun onEndOfSpeech() {
                    currentState = SttState.PROCESSING
                }

                override fun onError(error: Int) {
                    currentState = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SttState.IDLE
                        SpeechRecognizer.ERROR_AUDIO,
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SttState.ERROR
                        SpeechRecognizer.ERROR_CLIENT -> SttState.ERROR
                        else -> SttState.ERROR
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    currentTranscript = matches?.firstOrNull() ?: ""
                    currentState = SttState.READY
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialTranscript = matches?.firstOrNull() ?: ""
                }

                override fun onEvent(eventType: Int, params: Bundle?) { /* no-op */ }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    override suspend fun stop() {
        speechRecognizer?.stopListening()
        currentState = if (currentTranscript.isNotBlank()) SttState.READY else SttState.IDLE
    }

    override fun cancel() {
        speechRecognizer?.cancel()
        currentState = SttState.IDLE
        currentTranscript = ""
        partialTranscript = ""
    }

    override fun transcript(): String {
        return currentTranscript.ifBlank { partialTranscript }
    }

    override fun state(): SttState = currentState

    override fun provider(): SttProvider = SttProvider.LOCAL

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
