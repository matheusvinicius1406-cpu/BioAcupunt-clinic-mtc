package com.bioacupunt.observability

object SyncStatusMonitor {

    fun describe(status: SyncStatus, connectivity: com.bioacupunt.core.network.NetworkStatus): String = when (status) {
        is SyncStatus.Syncing -> "Sincronizando..."
        is SyncStatus.Error -> "Erro de sincronização. Toque para tentar novamente."
        else -> when (connectivity) {
            com.bioacupunt.core.network.NetworkStatus.OFFLINE -> "Offline – dados locais"
            else -> "Online"
        }
    }
}
