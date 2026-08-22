package com.bioacupunt.clinic.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for tongue observations.
 *
 * Stores structured tongue analysis data linked to a patient/encounter.
 * Vision output goes through DRAFT → professional review → CONFIRMED.
 * The vision model NEVER auto-confirms.
 *
 * ADDITIVE ONLY — no columns removed from existing tables.
 */
@Entity(
    tableName = "tongue_observations",
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("encounterId"),
        Index("status"),
        Index("mediaId"),
    ]
)
data class TongueObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long = 0,
    val mediaId: Long = 0,
    val observationId: Long = 0,

    // Tongue features
    val bodyColor: String = "",
    val bodyColorNotes: String = "",
    val shape: String = "",
    val shapeNotes: String = "",
    val coating: String = "",
    val coatingNotes: String = "",
    val moisture: String = "",
    val moistureNotes: String = "",
    val cracks: String = "",
    val marks: String = "",
    val movement: String = "",
    val specialFindings: String = "",

    // Regions
    val regionTip: String = "",
    val regionCenter: String = "",
    val regionRoot: String = "",
    val regionLeft: String = "",
    val regionRight: String = "",

    // Lifecycle
    val status: String = "DRAFT",
    val source: String = "MANUAL",

    // Vision provenance
    val visionModelName: String = "",
    val visionModelVersion: String = "",
    val visionConfidence: Double = 0.0,
    val preprocessingVersion: String = "",

    // Review
    val reviewedBy: String = "",
    val reviewedAt: String = "",
    val confirmedBy: String = "",
    val confirmedAt: String = "",

    // Timestamps
    val createdAt: String = "",
    val updatedAt: String = "",
    val deleted: Long = 0,
)
