package com.bioacupunt.observability

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Error(val cause: String? = null) : SyncStatus
}

class SyncStatusManager {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun markSyncing() { _status.value = SyncStatus.Syncing }

    fun markSuccess() { _status.value = SyncStatus.Idle }

    fun markError(cause: String? = null) { _status.value = SyncStatus.Error(cause) }

    fun markIdle() { _status.value = SyncStatus.Idle }
}
