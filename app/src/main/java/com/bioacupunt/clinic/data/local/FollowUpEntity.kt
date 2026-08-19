package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "follow_ups",
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
        Index("scheduledAt"),
        Index("updatedAt"),
    ],
)
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long? = null,
    val scheduledAt: String = "",
    val reason: String = "",
    val expectedFindings: String = "",
    val actualFindings: String = "",
    val status: String,
    val completedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
)
