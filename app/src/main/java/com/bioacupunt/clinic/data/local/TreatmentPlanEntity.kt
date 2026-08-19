package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "treatment_plans",
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
data class TreatmentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val goals: String = "",
    val principles: String = "",
    val itemsJson: String = "[]",
    val frequency: String = "",
    val duration: String = "",
    val followUpRecommendation: String = "",
    val status: String,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Boolean = false,
)
