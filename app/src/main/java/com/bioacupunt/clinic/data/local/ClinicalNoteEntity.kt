package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clinical_notes",
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
        Index("status"),
        Index("updatedAt"),
    ],
)
data class ClinicalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val format: String,
    val subjective: String = "",
    val objective: String = "",
    val assessment: String = "",
    val plan: String = "",
    val mtcAssessmentSummary: String = "",
    val referencesJson: String = "[]",
    val status: String,
    val createdBy: String = "",
    val finalizedBy: String? = null,
    val finalizedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
)
