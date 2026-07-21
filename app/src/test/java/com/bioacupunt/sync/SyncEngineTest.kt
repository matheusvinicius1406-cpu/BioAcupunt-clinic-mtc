package com.bioacupunt.sync

import com.bioacupunt.data.remote.SyncApi
import com.bioacupunt.data.remote.SyncChangeResultDto
import com.bioacupunt.data.remote.SyncPullResponseDto
import com.bioacupunt.data.remote.SyncPushRequestDto
import com.bioacupunt.data.remote.SyncPushResponseDto
import com.bioacupunt.data.remote.SyncRecordDto
import com.bioacupunt.sync.data.local.SyncConflictDao
import com.bioacupunt.sync.data.local.SyncConflictEntity
import com.bioacupunt.sync.data.local.SyncStateDao
import com.bioacupunt.sync.data.local.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the rules that make sync safe to leave running unattended.
 *
 * The one that matters most: **a record is marked synced only when the server
 * says it accepted it.** The previous worker dispatched on entity type with a
 * `when` that had no `else`, then marked the item SYNCED regardless — so every
 * appointment and every transaction was recorded as uploaded without a byte
 * being sent. An unsent record that knows it is unsent gets retried; one that
 * falsely believes it is safe is simply gone.
 */
@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    // ── Test doubles ─────────────────────────────────────────────────────

    private class FakeStateDao(var state: SyncStateEntity = SyncStateEntity()) : SyncStateDao {
        override suspend fun get(id: Int) = state
        override fun observe(id: Int): Flow<SyncStateEntity?> = flowOf(state)
        override suspend fun save(state: SyncStateEntity) { this.state = state }
        override suspend fun advanceCursor(rev: Long, at: String, id: Int) {
            state = state.copy(lastPulledRev = maxOf(state.lastPulledRev, rev), lastSyncAt = at)
        }
        override suspend fun recordError(error: String?, id: Int) {
            state = state.copy(lastError = error)
        }
    }

    private class FakeConflictDao : SyncConflictDao {
        val saved = mutableListOf<SyncConflictEntity>()
        val resolved = mutableListOf<Pair<Long, String>>()
        override fun observeUnresolved(): Flow<List<SyncConflictEntity>> = flowOf(saved)
        override fun observeUnresolvedCount(): Flow<Int> = flowOf(saved.size)
        override suspend fun getById(id: Long) = saved.firstOrNull { it.id == id }
        override suspend fun save(conflict: SyncConflictEntity): Long {
            saved.add(conflict.copy(id = saved.size + 1L)); return saved.size.toLong()
        }
        override suspend fun markResolved(id: Long, resolution: String, at: String) {
            resolved.add(id to resolution)
        }
    }

    private class RecordingWriter(
        private val pending: List<PendingChange> = emptyList(),
    ) : SyncEntityWriter {
        val markedSynced = mutableListOf<String>()
        val rebased = mutableListOf<String>()
        val applied = mutableListOf<SyncRecordDto>()
        override suspend fun pendingChanges() = pending
        override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) {
            markedSynced.add(clientId)
        }
        override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) {
            rebased.add(clientId)
        }
        override suspend fun applyRemote(record: SyncRecordDto) { applied.add(record) }
    }

    private class FakeApi(
        val pushResponse: (SyncPushRequestDto) -> SyncPushResponseDto = {
            SyncPushResponseDto(results = emptyList(), serverRev = 0)
        },
        val pullResponse: SyncPullResponseDto = SyncPullResponseDto(emptyList(), 0, false),
    ) : SyncApi {
        var lastPush: SyncPushRequestDto? = null
        override suspend fun push(request: SyncPushRequestDto): SyncPushResponseDto {
            lastPush = request
            return pushResponse(request)
        }
        override suspend fun pull(since: Long, limit: Int) = pullResponse
    }

    private fun change(clientId: String, serverId: Long? = null, baseRev: Long = 0) =
        PendingChange(
            clientId = clientId, serverId = serverId, baseRev = baseRev,
            deleted = false, payload = mapOf("name" to "Ana Lima"),
        )

    private fun engine(
        api: SyncApi,
        writers: Map<String, SyncEntityWriter>,
        stateDao: SyncStateDao = FakeStateDao(),
        conflictDao: SyncConflictDao = FakeConflictDao(),
    ) = SyncEngine(api, stateDao, conflictDao, writers) { "2026-07-18T10:00:00Z" }

    // ── The rule the old worker broke ────────────────────────────────────

    @Test
    fun `a record the server did not accept is never marked synced`() = runTest {
        val writer = RecordingWriter(listOf(change("c1")))
        val api = FakeApi(pushResponse = {
            SyncPushResponseDto(
                results = listOf(
                    SyncChangeResultDto(
                        clientId = "c1", entityType = "patient", status = "rejected",
                        reason = "amount_brl não é um valor numérico válido",
                    )
                ),
                serverRev = 5,
            )
        })

        val report = engine(api, mapOf("patient" to writer)).syncOnce()

        assertTrue(
            "a rejected record must stay pending so it can be retried",
            writer.markedSynced.isEmpty(),
        )
        assertEquals(1, report.rejected)
        assertEquals(0, report.pushed)
    }

    @Test
    fun `an entity type with no writer fails loudly instead of being skipped`() = runTest {
        // The exact shape of the old bug: a type nothing handles must not pass
        // silently through the engine and be reported as done.
        val api = FakeApi(pushResponse = {
            SyncPushResponseDto(
                results = listOf(
                    SyncChangeResultDto(clientId = "x1", entityType = "prontuario", status = "applied")
                ),
                serverRev = 1,
            )
        })
        val writer = RecordingWriter(listOf(change("c1")))

        val error = runCatching {
            engine(api, mapOf("patient" to writer)).syncOnce()
        }.exceptionOrNull()

        assertTrue(
            "an unhandled entity type must throw, not be ignored",
            error is SyncEngine.UnknownEntityType,
        )
    }

    @Test
    fun `an accepted record is marked synced with the server revision`() = runTest {
        val writer = RecordingWriter(listOf(change("c1")))
        val api = FakeApi(pushResponse = {
            SyncPushResponseDto(
                results = listOf(
                    SyncChangeResultDto(
                        clientId = "c1", entityType = "patient", status = "applied",
                        serverId = 42, rev = 7,
                    )
                ),
                serverRev = 7,
            )
        })

        val report = engine(api, mapOf("patient" to writer)).syncOnce()

        assertEquals(listOf("c1"), writer.markedSynced)
        assertEquals(1, report.pushed)
    }

    // ── Conflicts are never resolved automatically ───────────────────────

    @Test
    fun `a conflict stores both versions and leaves the record pending`() = runTest {
        val writer = RecordingWriter(listOf(change("c1", serverId = 42, baseRev = 3)))
        val conflicts = FakeConflictDao()
        val api = FakeApi(pushResponse = {
            SyncPushResponseDto(
                results = listOf(
                    SyncChangeResultDto(
                        clientId = "c1", entityType = "patient", status = "conflict",
                        serverId = 42, rev = 9,
                        serverPayload = mapOf("name" to "Ana Lima (tablet)"),
                    )
                ),
                serverRev = 9,
            )
        })

        val report = engine(api, mapOf("patient" to writer), conflictDao = conflicts).syncOnce()

        assertEquals(1, report.conflicted)
        assertTrue(
            "a conflicted record must NOT be marked synced — her edit is still unsent",
            writer.markedSynced.isEmpty(),
        )
        val stored = conflicts.saved.single()
        // Both sides are kept. The engine picks neither.
        assertTrue(stored.localPayloadJson.contains("Ana Lima"))
        assertTrue(stored.serverPayloadJson.contains("tablet"))
        assertNull("unresolved until she chooses", stored.resolvedAt)
    }

    @Test
    fun `resolving in favour of local rebases so the retry is not stale`() = runTest {
        val writer = RecordingWriter()
        val conflicts = FakeConflictDao()
        conflicts.save(
            SyncConflictEntity(
                entityType = "patient", clientId = "c1", serverId = 42, serverRev = 9,
                localPayloadJson = """{"name":"local"}""",
                serverPayloadJson = """{"name":"server"}""",
                detectedAt = "2026-07-18T10:00:00Z",
            )
        )

        engine(FakeApi(), mapOf("patient" to writer), conflictDao = conflicts)
            .resolveKeepingLocal(1L)

        // Rebased onto the server's revision, keeping local values — otherwise
        // the next push would be refused as stale all over again.
        assertEquals(listOf("c1"), writer.rebased)
        assertEquals(listOf(1L to "LOCAL"), conflicts.resolved)
    }

    @Test
    fun `resolving in favour of the server applies the server payload`() = runTest {
        val writer = RecordingWriter()
        val conflicts = FakeConflictDao()
        conflicts.save(
            SyncConflictEntity(
                entityType = "patient", clientId = "c1", serverId = 42, serverRev = 9,
                localPayloadJson = """{"name":"local"}""",
                serverPayloadJson = """{"name":"server"}""",
                detectedAt = "2026-07-18T10:00:00Z",
            )
        )

        engine(FakeApi(), mapOf("patient" to writer), conflictDao = conflicts)
            .resolveKeepingServer(1L)

        assertEquals("server", writer.applied.single().payload["name"])
        assertEquals(listOf(1L to "SERVER"), conflicts.resolved)
    }

    // ── Push/pull mechanics ──────────────────────────────────────────────

    @Test
    fun `a never-synced record sends no base revision`() = runTest {
        // base_rev 0 would read to the server as "I saw revision 0 of an
        // existing record" — a stale edit — rather than "this is new".
        val writer = RecordingWriter(listOf(change("c1", serverId = null, baseRev = 0)))
        val api = FakeApi()

        engine(api, mapOf("patient" to writer)).syncOnce()

        assertNull(api.lastPush!!.changes.single().baseRev)
    }

    @Test
    fun `an already-synced record sends the revision it was based on`() = runTest {
        val writer = RecordingWriter(listOf(change("c1", serverId = 42, baseRev = 3)))
        val api = FakeApi()

        engine(api, mapOf("patient" to writer)).syncOnce()

        assertEquals(3L, api.lastPush!!.changes.single().baseRev)
    }

    @Test
    fun `pulled records are applied and the cursor advances`() = runTest {
        val writer = RecordingWriter()
        val state = FakeStateDao()
        val api = FakeApi(
            pullResponse = SyncPullResponseDto(
                records = listOf(
                    SyncRecordDto("patient", serverId = 1, clientId = "a", rev = 4, payload = mapOf("name" to "Ana")),
                    SyncRecordDto("patient", serverId = 2, clientId = "b", rev = 6, payload = mapOf("name" to "Beto")),
                ),
                serverRev = 6,
                hasMore = false,
            )
        )

        val report = engine(api, mapOf("patient" to writer), stateDao = state).syncOnce()

        assertEquals(2, report.pulled)
        assertEquals(6L, state.state.lastPulledRev)
    }

    @Test
    fun `the cursor does not advance past a record that failed to apply`() = runTest {
        // Otherwise the failed record is never requested again: the cursor says
        // it arrived, the database says otherwise, and nothing reconciles them.
        val failing = object : SyncEntityWriter {
            override suspend fun pendingChanges() = emptyList<PendingChange>()
            override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) = Unit
            override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) = Unit
            override suspend fun applyRemote(record: SyncRecordDto) {
                if (record.rev >= 6L) error("simulated write failure")
            }
        }
        val state = FakeStateDao()
        val api = FakeApi(
            pullResponse = SyncPullResponseDto(
                records = listOf(
                    SyncRecordDto("patient", serverId = 1, clientId = "a", rev = 4),
                    SyncRecordDto("patient", serverId = 2, clientId = "b", rev = 6),
                ),
                serverRev = 6,
            )
        )

        engine(api, mapOf("patient" to failing), stateDao = state).syncOnce()

        assertEquals(
            "cursor must stop at the last record that actually landed",
            4L, state.state.lastPulledRev,
        )
    }

    @Test
    fun `push happens before pull`() = runTest {
        // Pulling first would bring down the server's older copy of a record
        // this device has just edited, and overwrite the edit before it is sent.
        val order = mutableListOf<String>()
        val writer = object : SyncEntityWriter {
            override suspend fun pendingChanges(): List<PendingChange> {
                order.add("push"); return emptyList()
            }
            override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) = Unit
            override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) = Unit
            override suspend fun applyRemote(record: SyncRecordDto) { order.add("apply") }
        }
        val api = object : SyncApi {
            override suspend fun push(request: SyncPushRequestDto) =
                SyncPushResponseDto(emptyList(), 0)
            override suspend fun pull(since: Long, limit: Int): SyncPullResponseDto {
                order.add("pull")
                return SyncPullResponseDto(emptyList(), 0, false)
            }
        }

        engine(api, mapOf("patient" to writer)).syncOnce()

        assertEquals(listOf("push", "pull"), order)
    }

    @Test
    fun `hasMore is reported so the caller comes straight back`() = runTest {
        val api = FakeApi(
            pullResponse = SyncPullResponseDto(
                records = listOf(SyncRecordDto("patient", serverId = 1, clientId = "a", rev = 2)),
                serverRev = 99,
                hasMore = true,
            )
        )
        val report = engine(api, mapOf("patient" to RecordingWriter())).syncOnce()
        assertTrue(report.hasMore)
    }

    @Test
    fun `a fresh device with no state pulls from zero`() = runTest {
        val state = FakeStateDao(SyncStateEntity(lastPulledRev = 0))
        val api = FakeApi()
        engine(api, mapOf("patient" to RecordingWriter()), stateDao = state).syncOnce()
        assertNotNull(state.state)
        assertEquals(0L, state.state.lastPulledRev)
    }
}
