package com.bioacupunt.observability

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Error(val cause: String? = null) : SyncStatus
}
