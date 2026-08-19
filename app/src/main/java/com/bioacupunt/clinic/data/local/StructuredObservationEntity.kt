package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structured_observations",
    foreignKeys = [
        ForeignKey(
            entity = EncounterEntity::class,
            parentColumns = ["id"],
            childColumns = ["encounterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("encounterId"),
        Index("type"),
        Index("status"),
        Index("source"),
        Index("updatedAt"),
    ],
)
data class StructuredObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val type: String,
    val content: String,
    val structuredDataJson: String = "{}",
    val status: String,
    val source: String,
    val sourceSpan: String? = null,
    val confidence: Double? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val confirmedBy: String? = null,
    val confirmedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
)
