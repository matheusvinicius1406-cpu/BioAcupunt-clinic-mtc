package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for pulse observations.
 *
 * Stores structured pulse analysis data linked to a patient/encounter.
 * MTC pulse positions: CUN (寸), GUAN (关), CHI (尺) for both wrists.
 *
 * ADDITIVE ONLY — no columns removed from existing tables.
 */
@Entity(
    tableName = "pulse_observations",
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("encounterId"),
        Index("status"),
    ]
)
data class PulseObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long = 0,
    val observationId: Long = 0,

    // Pulse measurements
    val depth: String = "",
    val rate: Int = 0,
    val strength: String = "",
    val width: String = "",
    val quality: String = "",
    val qualityNotes: String = "",

    // Position-specific findings
    val leftCun: String = "",
    val leftGuan: String = "",
    val leftChi: String = "",
    val rightCun: String = "",
    val rightGuan: String = "",
    val rightChi: String = "",

    // Features JSON (for device/AI detailed data)
    val featuresJson: String = "[]",

    // Lifecycle
    val status: String = "DRAFT",
    val source: String = "MANUAL",

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
