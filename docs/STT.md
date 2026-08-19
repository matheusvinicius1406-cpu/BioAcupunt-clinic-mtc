# Speech-to-Text — Voice Workflow

## Overview

The STT abstraction provides a provider-agnostic interface for voice input.
The domain never knows if it's running local or cloud. Priority: LOCAL.

## Architecture

```
Audio Input
    ↓
SpeechToTextEngine (interface)
    ├── AndroidSpeechToTextEngine (LOCAL — Android SpeechRecognizer)
    ├── FakeSpeechToTextEngine (testing)
    └── [Future: CloudSpeechToTextEngine]
    ↓
Transcript
    ↓
ClinicalNlpUseCase.extract()
    ↓
StructuredObservations (DRAFT)
    ↓
Professional Review
```

## States

| State | Description |
|---|---|
| `IDLE` | Not recording |
| `RECORDING` | Actively recording audio |
| `PROCESSING` | Converting audio to text |
| `READY` | Transcript available |
| `ERROR` | Something went wrong |
| `PERMISSION_DENIED` | RECORD_AUDIO not granted |

## Interface

```kotlin
interface SpeechToTextEngine {
    suspend fun start()
    suspend fun stop()
    fun cancel()
    fun transcript(): String
    fun state(): SttState
    fun provider(): SttProvider
}
```

## Privacy

- Audio stays local when using LOCAL provider
- Cloud provider requires explicit configuration and policy
- No audio is transmitted without user permission

## Files

- `clinic/domain/model/SpeechToText.kt` — interface + states
- `clinic/data/stt/AndroidSpeechToTextEngine.kt` — Android impl
- `clinic/data/stt/FakeSpeechToTextEngine.kt` — test double
