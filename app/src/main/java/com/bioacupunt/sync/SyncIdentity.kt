package com.bioacupunt.sync

import java.util.UUID

/**
 * The sync identity of one local row.
 *
 * Carried through the mappers so that saving a record preserves what the server
 * already knows about it. Losing these on an edit is not a cosmetic bug: a row
 * that comes back with a blank [clientId] collides with every other blank one on
 * the unique index, and a row that loses its [baseRev] pushes a stale revision
 * and is refused as a conflict the doctor never caused.
 */
data class SyncIdentity(
    val clientId: String,
    val serverId: Long? = null,
    val baseRev: Long = 0,
) {
    companion object {
        /**
         * Mints an identity for a record that has never existed before.
         *
         * A UUID rather than a row id: the row id is only unique on this device,
         * and two devices creating a patient offline would both produce id 4.
         * The server would read the second as an edit of the first, and one of
         * the two patients would quietly cease to exist.
         */
        fun new(): SyncIdentity = SyncIdentity(clientId = UUID.randomUUID().toString())

        /**
         * Carries an existing identity forward, minting one if the row predates
         * sync (or arrived from a path that never assigned one).
         */
        fun carryForward(clientId: String?, serverId: Long?, baseRev: Long): SyncIdentity =
            if (clientId.isNullOrBlank()) new().copy(serverId = serverId, baseRev = baseRev)
            else SyncIdentity(clientId, serverId, baseRev)
    }
}
