package com.bioacupunt.clinic.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for clinical media records.
 * Binary files are stored in secure app-internal storage; this table tracks metadata only.
 */
@Entity(
    tableName = "clinical_media",
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("encounterId"),
        Index("status"),
        Index("deleted"),
    ]
)
data class ClinicalMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    @ColumnInfo(defaultValue = "0")
    val encounterId: Long = 0,
    val type: String,
    val uri: String,
    val mimeType: String,
    @ColumnInfo(defaultValue = "")
    val originalName: String = "",
    @ColumnInfo(defaultValue = "0")
    val sizeBytes: Long = 0,
    @ColumnInfo(defaultValue = "")
    val hash: String = "",
    val source: String,
    val status: String,
    @ColumnInfo(defaultValue = "")
    val category: String = "",
    @ColumnInfo(defaultValue = "")
    val description: String = "",
    @ColumnInfo(defaultValue = "")
    val processingVersion: String = "",
    @ColumnInfo(defaultValue = "")
    val capturedAt: String = "",
    @ColumnInfo(defaultValue = "")
    val capturedBy: String = "",
    @ColumnInfo(defaultValue = "")
    val deviceInfo: String = "",
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0")
    val deleted: Long = 0,
)
