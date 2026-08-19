package com.bioacupunt.clinic

import com.bioacupunt.clinic.data.stt.FakeSpeechToTextEngine
import com.bioacupunt.clinic.domain.model.SttProvider
import com.bioacupunt.clinic.domain.model.SttState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SpeechToTextTest {

    @Test
    fun fakeEngine_initialState_idle() {
        val engine = FakeSpeechToTextEngine()
        assertEquals(SttState.IDLE, engine.state())
        assertEquals(SttProvider.LOCAL, engine.provider())
    }

    @Test
    fun fakeEngine_startStop_transcriptAvailable() = runTest {
        val engine = FakeSpeechToTextEngine(fakeTranscript = "Paciente relata insônia")
        engine.start()
        assertEquals(SttState.RECORDING, engine.state())

        engine.stop()
        assertEquals(SttState.READY, engine.state())
        assertEquals("Paciente relata insônia", engine.transcript())
    }

    @Test
    fun fakeEngine_cancel_clearsState() = runTest {
        val engine = FakeSpeechToTextEngine(fakeTranscript = "test")
        engine.start()
        engine.cancel()
        assertEquals(SttState.IDLE, engine.state())
        assertEquals("", engine.transcript())
    }

    @Test
    fun fakeEngine_failOnStart_errorState() = runTest {
        val engine = FakeSpeechToTextEngine(failOnStart = true)
        engine.start()
        assertEquals(SttState.ERROR, engine.state())
    }

    @Test
    fun fakeEngine_emptyTranscript_idleAfterStop() = runTest {
        val engine = FakeSpeechToTextEngine(fakeTranscript = "")
        engine.start()
        engine.stop()
        assertEquals(SttState.IDLE, engine.state())
    }

    @Test
    fun fakeEngine_provider_alwaysLocal() {
        val engine = FakeSpeechToTextEngine()
        assertEquals(SttProvider.LOCAL, engine.provider())
    }
}
