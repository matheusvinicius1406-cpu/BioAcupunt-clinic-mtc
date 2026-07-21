package com.bioacupunt.crm.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crm_patients",
    indices = [
        Index("tenantId"),
        Index("name"),
        Index("phone"),
        Index("stage"),
        Index("updatedAt"),
        Index("clientId", unique = true)
    ]
)
data class CrmPatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val birthDate: String = "",
    val stage: String = com.bioacupunt.crm.domain.model.PatientStage.FIRST_CONTACT.name,
    val totalSessions: Int = 0,
    val totalRevenueBrl: Double = 0.0,
    val lastVisit: String = "",
    val nextAppointment: String = "",
    val tags: String = "",
    val notes: String = "",
    val referralSource: String = "",
    val npsScore: Int? = null,
    val healthInsurance: String = "",
    val mainComplaint: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
    val lastModified: String = "",

    // ── Sync identity ────────────────────────────────────────────────────
    /**
     * Stable id generated on *this* device before the row ever reaches the
     * server. It is what makes a retried upload idempotent: if the push
     * succeeded but the reply was lost, the retry is recognised as the same
     * record instead of creating a second patient.
     */
    val clientId: String = "",
    /** The server's id, once known. Null means this row has never synced. */
    val serverId: Long? = null,
    /**
     * The server revision this row was last in agreement with. Sent on push so
     * the server can tell whether anyone else changed the record in between.
     */
    val baseRev: Long = 0
)
