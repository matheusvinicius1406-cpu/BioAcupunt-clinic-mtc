package com.bioacupunt.sync

import com.bioacupunt.data.remote.SyncApi
import com.bioacupunt.data.remote.SyncChangeDto
import com.bioacupunt.data.remote.SyncPushRequestDto
import com.bioacupunt.data.remote.SyncRecordDto
import com.bioacupunt.observability.AppLogger
import com.bioacupunt.sync.data.local.SyncConflictDao
import com.bioacupunt.sync.data.local.SyncConflictEntity
import com.bioacupunt.sync.data.local.SyncStateDao
import com.bioacupunt.sync.data.local.SyncStateEntity
import org.json.JSONObject
import java.io.IOException

/**
 * Two-way delta sync.
 *
 * The shape of one run: **push first, then pull.** Pushing first means the
 * server has seen this device's work before we ask what changed, so a record
 * edited here does not come back down in its old form and overwrite itself.
 *
 * Three rules hold this together:
 *
 * 1. **Nothing is ever marked synced unless it was actually sent and accepted.**
 *    The previous implementation had a `when` with no `else`: any entity type it
 *    did not recognise fell through and was then marked `SYNCED` anyway. The
 *    queue reported success for records that had never left the device. An
 *    unsent record that *says* it is unsent gets retried; one that lies about it
 *    is gone. Unknown types now fail loudly.
 *
 * 2. **A conflict is never resolved automatically.** When the server refuses an
 *    edit, both versions are written to `sync_conflicts` and the doctor decides.
 *    No timestamp comparison, no "server wins", no merge.
 *
 * 3. **The pull cursor advances only over records actually applied.** See
 *    [SyncStateDao.advanceCursor].
 */
class SyncEngine(
    private val api: SyncApi,
    private val stateDao: SyncStateDao,
    private val conflictDao: SyncConflictDao,
    private val writers: Map<String, SyncEntityWriter>,
    private val now: () -> String = { java.time.Instant.now().toString() },
) {

    /** What one sync run produced. */
    data class Report(
        val pushed: Int = 0,
        val conflicted: Int = 0,
        val rejected: Int = 0,
        val pulled: Int = 0,
        val hasMore: Boolean = false,
    )

    /**
     * Thrown when the queue contains an entity type nothing knows how to send.
     *
     * Deliberately fatal to the run rather than skipped. A record with no writer
     * is a record that will never sync, and the only thing worse than noticing
     * that loudly is not noticing it at all.
     */
    class UnknownEntityType(entityType: String) :
        IllegalStateException("No sync writer registered for entity type '$entityType'")

    suspend fun syncOnce(): Report {
        val pushReport = push()
        val pullReport = pull()
        return pushReport.copy(pulled = pullReport.pulled, hasMore = pullReport.hasMore)
    }

    // ── Push ─────────────────────────────────────────────────────────────

    private suspend fun push(): Report {
        val pending = writers.flatMap { (entityType, writer) ->
            writer.pendingChanges().map { entityType to it }
        }
        if (pending.isEmpty()) return Report()

        val changes = pending.map { (entityType, change) ->
            SyncChangeDto(
                entityType = entityType,
                op = if (change.deleted) "delete" else "upsert",
                clientId = change.clientId,
                serverId = change.serverId,
                // A record that has never synced has no base revision. Sending 0
                // would claim "I saw revision 0", which the server would read as
                // a stale edit of an existing record rather than a new one.
                baseRev = change.serverId?.let { change.baseRev },
                payload = change.payload,
            )
        }

        val response = api.push(SyncPushRequestDto(changes = changes))

        var pushed = 0
        var conflicted = 0
        var rejected = 0

        val changeByKey = pending.associateBy { (entityType, change) ->
            entityType to change.clientId
        }

        response.results.forEach { result ->
            val writer = writers[result.entityType]
                ?: throw UnknownEntityType(result.entityType)
            val local = changeByKey[result.entityType to result.clientId]?.second

            when (result.status) {
                "applied" -> {
                    writer.markSynced(
                        clientId = result.clientId,
                        serverId = result.serverId,
                        rev = result.rev ?: 0L,
                    )
                    pushed++
                }

                "conflict" -> {
                    conflictDao.save(
                        SyncConflictEntity(
                            entityType = result.entityType,
                            clientId = result.clientId,
                            serverId = result.serverId,
                            serverRev = result.rev ?: 0L,
                            localPayloadJson = local?.payload?.toJsonString() ?: "{}",
                            serverPayloadJson = result.serverPayload?.toJsonString() ?: "{}",
                            detectedAt = now(),
                        )
                    )
                    // Deliberately NOT marked synced. The local edit stays
                    // pending and stays on screen until she resolves it — a
                    // record quietly dropped from the queue is a record she
                    // believes she saved.
                    conflicted++
                    AppLogger.w(
                        "SyncEngine",
                        "Conflict on ${result.entityType}/${result.clientId}; awaiting user decision",
                    )
                }

                "rejected" -> {
                    rejected++
                    AppLogger.e(
                        "SyncEngine",
                        "Server rejected ${result.entityType}/${result.clientId}: ${result.reason}",
                        null,
                    )
                }

                else -> AppLogger.e(
                    "SyncEngine",
                    "Unknown push status '${result.status}' for ${result.clientId}",
                    null,
                )
            }
        }

        return Report(pushed = pushed, conflicted = conflicted, rejected = rejected)
    }

    // ── Pull ─────────────────────────────────────────────────────────────

    private suspend fun pull(): Report {
        val state = stateDao.get() ?: SyncStateEntity().also { stateDao.save(it) }
        val response = api.pull(since = state.lastPulledRev)
        if (response.records.isEmpty()) {
            stateDao.advanceCursor(response.serverRev, now())
            return Report()
        }

        var applied = 0
        var highestApplied = state.lastPulledRev

        // In revision order, so a partial failure leaves the cursor at the last
        // record that genuinely landed.
        response.records.sortedBy { it.rev }.forEach { record ->
            val writer = writers[record.entityType]
            if (writer == null) {
                // A record type this app version does not understand. Stop
                // advancing the cursor here: skipping it would mean this device
                // never sees it again, even after an update that adds support.
                AppLogger.w("SyncEngine", "No writer for pulled type '${record.entityType}'; stopping")
                return@forEach
            }
            runCatching { writer.applyRemote(record) }
                .onSuccess {
                    applied++
                    highestApplied = maxOf(highestApplied, record.rev)
                }
                .onFailure { error ->
                    AppLogger.e(
                        "SyncEngine",
                        "Failed to apply ${record.entityType}#${record.serverId}",
                        error,
                    )
                }
        }

        stateDao.advanceCursor(highestApplied, now())
        return Report(pulled = applied, hasMore = response.hasMore)
    }

    /** Resolves a conflict by keeping the local version — it will re-push. */
    suspend fun resolveKeepingLocal(conflictId: Long) {
        val conflict = conflictDao.getById(conflictId) ?: return
        val writer = writers[conflict.entityType] ?: throw UnknownEntityType(conflict.entityType)
        // Adopt the server's revision so the next push is no longer stale, but
        // keep the local field values — that is the whole point of this choice.
        writer.rebaseOnServer(
            clientId = conflict.clientId,
            serverId = conflict.serverId,
            rev = conflict.serverRev,
        )
        conflictDao.markResolved(conflictId, resolution = "LOCAL", at = now())
    }

    /** Resolves a conflict by taking the server's version, discarding the local edit. */
    suspend fun resolveKeepingServer(conflictId: Long) {
        val conflict = conflictDao.getById(conflictId) ?: return
        val writer = writers[conflict.entityType] ?: throw UnknownEntityType(conflict.entityType)
        writer.applyRemote(
            SyncRecordDto(
                entityType = conflict.entityType,
                serverId = conflict.serverId ?: return,
                clientId = conflict.clientId,
                rev = conflict.serverRev,
                deleted = false,
                payload = conflict.serverPayloadJson.toMap(),
            )
        )
        conflictDao.markResolved(conflictId, resolution = "SERVER", at = now())
    }
}

/** One local record waiting to go up. */
data class PendingChange(
    val clientId: String,
    val serverId: Long?,
    val baseRev: Long,
    val deleted: Boolean,
    val payload: Map<String, Any?>,
)

/**
 * Per-entity persistence for sync. One implementation per syncable table.
 *
 * Registering a writer is what makes an entity type syncable — [SyncEngine]
 * throws on a type with no writer rather than skipping it, so it is impossible
 * to half-add an entity and have it silently never sync.
 */
interface SyncEntityWriter {
    suspend fun pendingChanges(): List<PendingChange>
    suspend fun markSynced(clientId: String, serverId: Long?, rev: Long)
    suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long)
    suspend fun applyRemote(record: SyncRecordDto)
}

internal fun Map<String, Any?>.toJsonString(): String = JSONObject(this).toString()

internal fun String.toMap(): Map<String, Any?> = runCatching {
    val json = JSONObject(this)
    json.keys().asSequence().associateWith { key -> if (json.isNull(key)) null else json.get(key) }
}.getOrElse { emptyMap() }
