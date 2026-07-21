package com.bioacupunt.sync

import com.bioacupunt.agenda.data.local.AppointmentDao
import com.bioacupunt.agenda.data.local.AppointmentEntity
import com.bioacupunt.crm.data.local.CrmPatientDao
import com.bioacupunt.crm.data.local.CrmPatientEntity
import com.bioacupunt.data.remote.SyncRecordDto
import com.bioacupunt.financeiro.data.local.TransacaoDao
import com.bioacupunt.financeiro.data.local.TransacaoEntity

/**
 * Entity-specific sync persistence.
 *
 * Each writer owns the translation between the local Room row and the wire
 * payload the server understands, in both directions. Field names on the wire
 * are the *server's* (snake_case, `scheduled_at`), because the server schema is
 * the shared contract between devices; the local column names are this app's
 * private business.
 *
 * A note on what these deliberately do NOT do: none of them compares timestamps
 * to decide whether the local or remote version is newer. That decision belongs
 * to the doctor, via SyncEngine's conflict path. A writer that quietly preferred
 * one side would defeat the entire design.
 */

private fun Map<String, Any?>.str(key: String, fallback: String = ""): String =
    this[key]?.toString() ?: fallback

private fun Map<String, Any?>.long(key: String): Long? =
    when (val value = this[key]) {
        null -> null
        is Number -> value.toLong()
        else -> value.toString().toDoubleOrNull()?.toLong()
    }

private fun Map<String, Any?>.double(key: String, fallback: Double = 0.0): Double =
    when (val value = this[key]) {
        null -> fallback
        is Number -> value.toDouble()
        else -> value.toString().toDoubleOrNull() ?: fallback
    }

private fun Map<String, Any?>.bool(key: String, fallback: Boolean = false): Boolean =
    when (val value = this[key]) {
        null -> fallback
        is Boolean -> value
        is Number -> value.toInt() != 0
        else -> value.toString().equals("true", ignoreCase = true)
    }

/** "2026-07-18T09:00:00Z" → date "2026-07-18", time "09:00". */
private fun splitIsoInstant(value: String): Pair<String, String> {
    val date = value.substringBefore('T').ifBlank { value }
    val time = value.substringAfter('T', "").take(5)
    return date to time
}

class CrmPatientSyncWriter(
    private val dao: CrmPatientDao,
    private val tenantId: () -> Long,
    private val now: () -> String = { java.time.Instant.now().toString() },
) : SyncEntityWriter {

    override suspend fun pendingChanges(): List<PendingChange> =
        dao.getPendingSync().map { entity ->
            PendingChange(
                clientId = entity.clientId,
                serverId = entity.serverId,
                baseRev = entity.baseRev,
                deleted = entity.deleted,
                payload = mapOf(
                    "name" to entity.name,
                    "phone" to entity.phone,
                    "email" to entity.email,
                    "stage" to entity.stage,
                    "notes" to entity.notes,
                    "status" to "ACTIVE",
                ),
            )
        }

    override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) =
        dao.markSynced(clientId, serverId, rev)

    override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) =
        dao.rebaseOnServer(clientId, serverId, rev)

    override suspend fun applyRemote(record: SyncRecordDto) {
        // Match on client_id first: this may be a record *this* device created,
        // coming back with its server id for the first time. Matching on
        // serverId alone would fail to recognise it and insert a duplicate.
        val existing = record.clientId?.let { dao.getByClientId(it) }
            ?: dao.getByServerId(record.serverId)

        val payload = record.payload
        val entity = CrmPatientEntity(
            id = existing?.id ?: 0L,
            tenantId = existing?.tenantId ?: tenantId(),
            name = payload.str("name", existing?.name ?: ""),
            phone = payload.str("phone", existing?.phone ?: ""),
            email = payload.str("email", existing?.email ?: ""),
            birthDate = existing?.birthDate ?: "",
            stage = payload.str("stage", existing?.stage ?: "FIRST_CONTACT"),
            totalSessions = existing?.totalSessions ?: 0,
            totalRevenueBrl = existing?.totalRevenueBrl ?: 0.0,
            lastVisit = existing?.lastVisit ?: "",
            nextAppointment = existing?.nextAppointment ?: "",
            tags = existing?.tags ?: "",
            notes = payload.str("notes", existing?.notes ?: ""),
            referralSource = existing?.referralSource ?: "",
            npsScore = existing?.npsScore,
            healthInsurance = existing?.healthInsurance ?: "",
            mainComplaint = existing?.mainComplaint ?: "",
            createdAt = existing?.createdAt ?: now(),
            updatedAt = now(),
            // False: this row now matches the server exactly, so re-uploading it
            // would be a no-op push that could only produce a spurious conflict.
            pendingSync = false,
            deleted = record.deleted,
            lastModified = now(),
            clientId = record.clientId ?: existing?.clientId ?: "server-${record.serverId}",
            serverId = record.serverId,
            baseRev = record.rev,
        )
        dao.save(entity)
    }
}

class AppointmentSyncWriter(
    private val dao: AppointmentDao,
    private val patientDao: CrmPatientDao,
    private val tenantId: () -> Long,
    private val now: () -> String = { java.time.Instant.now().toString() },
) : SyncEntityWriter {

    override suspend fun pendingChanges(): List<PendingChange> =
        dao.getPendingSync().map { entity ->
            // The server keys appointments by *its* patient id, not the local
            // row id. A patient that has not synced yet has no server id, so the
            // appointment cannot be described to the server yet — it stays
            // pending and goes up on a later run, once its patient exists there.
            val patientServerId = patientDao.getByServerId(entity.patientId)?.serverId
                ?: patientDao.getPendingSync()
                    .firstOrNull { it.id == entity.patientId }?.serverId

            PendingChange(
                clientId = entity.clientId,
                serverId = entity.serverId,
                baseRev = entity.baseRev,
                deleted = entity.deleted,
                payload = mapOf(
                    "patient_id" to patientServerId,
                    "scheduled_at" to "${entity.date}T${entity.time.ifBlank { "00:00" }}:00Z",
                    "status" to entity.status,
                    "notes" to entity.notes,
                    "value_brl" to entity.valueBrl,
                    "paid" to entity.paid,
                ),
            )
        }.filter { it.payload["patient_id"] != null }

    override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) =
        dao.markSynced(clientId, serverId, rev)

    override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) =
        dao.rebaseOnServer(clientId, serverId, rev)

    override suspend fun applyRemote(record: SyncRecordDto) {
        val existing = record.clientId?.let { dao.getByClientId(it) }
            ?: dao.getByServerId(record.serverId)
        val payload = record.payload

        // Translate the server's patient id back to the local row id. If the
        // patient has not arrived yet, skip: applying the appointment now would
        // violate the foreign key. It will be retried on the next pull, by which
        // time the patient (a lower revision) will have landed.
        val serverPatientId = payload.long("patient_id")
        val localPatientId = serverPatientId?.let { patientDao.getByServerId(it)?.id }
            ?: existing?.patientId
            ?: return

        val (date, time) = splitIsoInstant(payload.str("scheduled_at"))

        dao.save(
            AppointmentEntity(
                id = existing?.id ?: 0L,
                tenantId = existing?.tenantId ?: tenantId(),
                patientId = localPatientId,
                patientName = existing?.patientName
                    ?: patientDao.getByServerId(serverPatientId ?: -1L)?.name
                    ?: "",
                date = date.ifBlank { existing?.date ?: "" },
                time = time.ifBlank { existing?.time ?: "" },
                durationMin = existing?.durationMin ?: 60,
                type = existing?.type ?: "ACUPUNCTURE",
                status = payload.str("status", existing?.status ?: "SCHEDULED"),
                notes = payload.str("notes", existing?.notes ?: ""),
                sessionNumber = existing?.sessionNumber ?: 1,
                reminderMinutesBefore = existing?.reminderMinutesBefore ?: 30,
                valueBrl = payload.double("value_brl", existing?.valueBrl ?: 0.0),
                paid = payload.bool("paid", existing?.paid ?: false),
                createdAt = existing?.createdAt ?: now(),
                updatedAt = now(),
                pendingSync = false,
                deleted = record.deleted,
                lastModified = now(),
                clientId = record.clientId ?: existing?.clientId ?: "server-${record.serverId}",
                serverId = record.serverId,
                baseRev = record.rev,
            )
        )
    }
}

class TransacaoSyncWriter(
    private val dao: TransacaoDao,
    private val patientDao: CrmPatientDao,
    private val now: () -> String = { java.time.Instant.now().toString() },
) : SyncEntityWriter {

    override suspend fun pendingChanges(): List<PendingChange> =
        dao.getPendingSync().map { entity ->
            val patientServerId = entity.patientId?.let { local ->
                patientDao.getPendingSync().firstOrNull { it.id == local }?.serverId
                    ?: patientDao.getByServerId(local)?.serverId
            }
            PendingChange(
                clientId = entity.clientId,
                serverId = entity.serverId,
                baseRev = entity.baseRev,
                deleted = entity.deleted,
                payload = mapOf(
                    "patient_id" to patientServerId,
                    "amount_brl" to entity.amountBrl,
                    "occurred_on" to entity.date,
                    "type" to entity.type,
                    "method" to entity.method,
                    "category" to entity.category,
                    "status" to entity.status,
                    "notes" to entity.notes,
                ),
            )
        }

    override suspend fun markSynced(clientId: String, serverId: Long?, rev: Long) =
        dao.markSynced(clientId, serverId, rev)

    override suspend fun rebaseOnServer(clientId: String, serverId: Long?, rev: Long) =
        dao.rebaseOnServer(clientId, serverId, rev)

    override suspend fun applyRemote(record: SyncRecordDto) {
        val existing = record.clientId?.let { dao.getByClientId(it) }
            ?: dao.getByServerId(record.serverId)
        val payload = record.payload
        val localPatientId = payload.long("patient_id")?.let { patientDao.getByServerId(it)?.id }
            ?: existing?.patientId

        dao.save(
            TransacaoEntity(
                id = existing?.id ?: 0L,
                patientId = localPatientId,
                appointmentId = existing?.appointmentId,
                amountBrl = payload.double("amount_brl", existing?.amountBrl ?: 0.0),
                date = payload.str("occurred_on", existing?.date ?: ""),
                type = payload.str("type", existing?.type ?: "PAGAMENTO"),
                method = payload.str("method", existing?.method ?: "PIX"),
                category = payload.str("category", existing?.category ?: "SESSÃO"),
                status = payload.str("status", existing?.status ?: "PAGO"),
                notes = payload.str("notes", existing?.notes ?: ""),
                pendingSync = false,
                deleted = record.deleted,
                createdAt = existing?.createdAt ?: now(),
                updatedAt = now(),
                lastModified = now(),
                clientId = record.clientId ?: existing?.clientId ?: "server-${record.serverId}",
                serverId = record.serverId,
                baseRev = record.rev,
            )
        )
    }
}
