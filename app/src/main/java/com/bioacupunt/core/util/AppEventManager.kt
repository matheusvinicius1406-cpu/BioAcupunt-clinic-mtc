package com.bioacupunt.core.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface AppEvent {
    data class PatientCreated(val patientId: Long) : AppEvent
    data class AppointmentFinished(val appointmentId: Long) : AppEvent
    data object SyncCompleted : AppEvent
    data object Logout : AppEvent
    data class BiometricAuthFailed(val reason: String) : AppEvent
    data class SyncError(val cause: Throwable?) : AppEvent
}

object AppEventManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun post(event: AppEvent) {
        scope.launch { _events.emit(event) }
    }

    fun observe(
        owner: LifecycleOwner,
        onEvent: suspend (AppEvent) -> Unit
    ) {
        val job = scope.launch {
            events.collect { onEvent(it) }
        }
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                job.cancel()
                owner.lifecycle.removeObserver(this)
            }
        })
    }
}
