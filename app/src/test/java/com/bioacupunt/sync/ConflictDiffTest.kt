package com.bioacupunt.sync

import com.bioacupunt.sync.data.local.SyncConflictEntity
import com.bioacupunt.sync.presentation.toItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The conflict screen's job is to let the doctor see exactly what differs
 * between two versions of a record. These guard the ways that comparison can
 * quietly mislead her.
 */
@RunWith(RobolectricTestRunner::class)
class ConflictDiffTest {

    private fun conflict(local: String, server: String) = SyncConflictEntity(
        id = 1,
        entityType = "patient",
        clientId = "c1",
        serverId = 42,
        serverRev = 9,
        localPayloadJson = local,
        serverPayloadJson = server,
        detectedAt = "2026-07-18T10:00:00Z",
    )

    @Test
    fun `a field present on only one side is still shown`() {
        // The dangerous case: iterating over one payload's keys would silently
        // hide a note that exists on the other side only — so she would choose
        // between two versions without being shown the thing that differs.
        val item = conflict(
            local = """{"name":"Ana Lima","notes":"queixa nova"}""",
            server = """{"name":"Ana Lima"}""",
        ).toItem()

        val notes = item.fields.single { it.label == "Observações" }
        assertEquals("queixa nova", notes.localValue)
        assertEquals("", notes.serverValue)
        assertTrue("a field only one side has is a difference", notes.differs)
    }

    @Test
    fun `identical fields are not marked as differing`() {
        val item = conflict(
            local = """{"name":"Ana Lima","phone":"11999990000"}""",
            server = """{"name":"Ana Lima","phone":"11999990000"}""",
        ).toItem()

        assertTrue(item.fields.none { it.differs })
    }

    @Test
    fun `field labels are translated for the doctor`() {
        val item = conflict(
            local = """{"name":"Ana","scheduled_at":"2026-07-18T09:00:00Z"}""",
            server = """{"name":"Ana","scheduled_at":"2026-07-18T10:00:00Z"}""",
        ).toItem()

        // She should read "Data e hora", not "scheduled_at".
        assertTrue(item.fields.any { it.label == "Data e hora" })
    }

    @Test
    fun `an unknown field falls back to its raw key rather than disappearing`() {
        // A newer app version may send a field this screen has no label for.
        // Hiding it would mean she resolves a conflict without seeing part of it.
        val item = conflict(
            local = """{"campo_novo":"valor local"}""",
            server = """{"campo_novo":"valor servidor"}""",
        ).toItem()

        val field = item.fields.single()
        assertEquals("campo_novo", field.label)
        assertTrue(field.differs)
    }

    @Test
    fun `malformed json degrades to an empty side instead of crashing`() {
        // A corrupted payload must not take down the screen that exists to
        // rescue her data.
        val item = conflict(local = "not json at all", server = """{"name":"Ana"}""").toItem()

        assertEquals("Ana", item.fields.single { it.label == "Nome" }.serverValue)
        assertEquals("", item.fields.single { it.label == "Nome" }.localValue)
    }

    @Test
    fun `the title prefers a human name over a record id`() {
        val item = conflict(local = """{"name":"Ana Lima"}""", server = """{"name":"Ana Lima"}""").toItem()
        assertEquals("Ana Lima", item.title)
    }

    @Test
    fun `a record with no name still gets an identifiable title`() {
        val item = conflict(local = """{"amount_brl":"150.0"}""", server = """{"amount_brl":"200.0"}""").toItem()
        assertTrue("must not render a blank heading", item.title.isNotBlank())
    }
}
