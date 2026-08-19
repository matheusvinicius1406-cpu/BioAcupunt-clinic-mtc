package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "encounters",
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("status"),
        Index("patientId", "startedAt"),
        Index("updatedAt"),
    ],
)
data class EncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val status: String,
    val type: String,
    val startedAt: String = "",
    val endedAt: String = "",
    val practitionerId: String = "",
    val reason: String = "",
    val appointmentId: Long? = null,
    val currentAssessmentId: Long? = null,
    val currentNoteId: Long? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
)

