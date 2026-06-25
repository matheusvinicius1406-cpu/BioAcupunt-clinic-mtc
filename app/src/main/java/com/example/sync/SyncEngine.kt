package com.example.sync

import java.util.UUID

enum class SyncState {
    PENDING, SYNCED, CONFLICT, FAILED
}

enum class ConflictResolutionStrategy {
    CLIENT_WINS, SERVER_WINS, MERGE_CUSTOM
}

data class SyncPayload(
    val entityId: String,
    val entityType: String, // "PATIENT", "DIAGNOSIS", "INVOICE", "EVOLUTION"
    val jsonPayload: String,
    val version: Int,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

data class SyncQueueEntry(
    val id: String = UUID.randomUUID().toString(),
    val payload: SyncPayload,
    val state: SyncState = SyncState.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

interface RemoteSyncService {
    suspend fun pushToServer(payload: SyncPayload): SyncResult
}

sealed interface SyncResult {
    object Success : SyncResult
    data class Conflict(val serverVersion: Int, val serverPayload: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

class ConflictResolver {
    fun resolve(
        client: SyncPayload,
        serverPayload: String,
        strategy: ConflictResolutionStrategy
    ): SyncPayload {
        return when (strategy) {
            ConflictResolutionStrategy.CLIENT_WINS -> client.copy(version = client.version + 1)
            ConflictResolutionStrategy.SERVER_WINS -> SyncPayload(
                entityId = client.entityId,
                entityType = client.entityType,
                jsonPayload = serverPayload,
                version = client.version + 1
            )
            ConflictResolutionStrategy.MERGE_CUSTOM -> {
                // Combine both payloads or flags (Simplified merge)
                SyncPayload(
                    entityId = client.entityId,
                    entityType = client.entityType,
                    jsonPayload = "{\"merged\": true, \"client\": ${client.jsonPayload}, \"server\": $serverPayload}",
                    version = client.version + 1
                )
            }
        }
    }
}

class SyncEngine(
    private val remoteService: RemoteSyncService,
    private val conflictResolver: ConflictResolver
) {
    private val queue = mutableListOf<SyncQueueEntry>()
    private val maxRetries = 3

    fun queueChange(payload: SyncPayload) {
        queue.add(SyncQueueEntry(payload = payload))
    }

    fun getPendingEntries(): List<SyncQueueEntry> = queue.filter { it.state == SyncState.PENDING }

    suspend fun runSyncCycle() {
        val pending = queue.filter { it.state == SyncState.PENDING || it.state == SyncState.FAILED }
        
        pending.forEach { entry ->
            try {
                val idx = queue.indexOf(entry)
                if (idx == -1) return@forEach

                val result = remoteService.pushToServer(entry.payload)
                when (result) {
                    is SyncResult.Success -> {
                        queue[idx] = entry.copy(state = SyncState.SYNCED)
                    }
                    is SyncResult.Conflict -> {
                        // Resolve conflict using client wins strategy as default for local offline-first
                        val resolvedPayload = conflictResolver.resolve(
                            client = entry.payload,
                            serverPayload = result.serverPayload,
                            strategy = ConflictResolutionStrategy.CLIENT_WINS
                        )
                        queue[idx] = entry.copy(
                            payload = resolvedPayload,
                            state = SyncState.PENDING
                        )
                    }
                    is SyncResult.Error -> {
                        val nextRetry = entry.retryCount + 1
                        val nextState = if (nextRetry >= maxRetries) SyncState.FAILED else SyncState.PENDING
                        queue[idx] = entry.copy(
                            state = nextState,
                            retryCount = nextRetry,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                val idx = queue.indexOf(entry)
                if (idx != -1) {
                    queue[idx] = entry.copy(
                        state = SyncState.FAILED,
                        errorMessage = e.message
                    )
                }
            }
        }
    }
}

class MockRemoteSyncService : RemoteSyncService {
    var simulateConflict = false
    var simulateError = false

    override suspend fun pushToServer(payload: SyncPayload): SyncResult {
        return when {
            simulateConflict -> SyncResult.Conflict(
                serverVersion = payload.version + 1,
                serverPayload = "{\"server_state\": \"STABLE_MOCK\"}"
            )
            simulateError -> SyncResult.Error("Network Timeout")
            else -> SyncResult.Success
        }
    }
}
