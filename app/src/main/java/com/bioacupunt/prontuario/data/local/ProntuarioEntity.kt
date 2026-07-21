package com.bioacupunt.prontuario.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prontuarios",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("updatedAt")]
)
data class ProntuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val summary: String = "",
    val mainComplaint: String = "",
    val diagnosis: String = "",
    val treatmentPlan: String = "",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(
    tableName = "prontuario_entries",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("date"), Index("type")]
)
data class ProntuarioEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val doctorName: String = "",
    val date: String = "",
    val type: String = com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.EVOLUTION.name,
    val body: String = "",
    val attachmentsJson: String = "[]",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)
