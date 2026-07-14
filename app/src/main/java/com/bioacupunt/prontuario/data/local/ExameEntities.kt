package com.bioacupunt.prontuario.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vital_signs",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.patient.data.local.PatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("recordedAt")]
)
data class VitalSignEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val label: String,
    val value: String,
    val recordedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Entity(
    tableName = "lab_exams",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.patient.data.local.PatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("date")]
)
data class LabExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val date: String = "",
    val resultTag: String = "PENDING",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.patient.data.local.PatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId")]
)
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val info: String = "",
    val active: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Entity(
    tableName = "allergies",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.patient.data.local.PatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId")]
)
data class AllergyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val description: String,
    val createdAt: String = "",
    val updatedAt: String = "",
)
