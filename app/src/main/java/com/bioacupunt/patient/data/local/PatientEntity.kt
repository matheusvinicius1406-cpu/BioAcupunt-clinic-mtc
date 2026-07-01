package com.bioacupunt.patient.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bioacupunt.patient.domain.model.Patient

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val name: String,
    val document: String?,
    val createdAt: String,
    val updatedAt: String,
    val status: String,
    val pendingSync: Boolean = false
)

fun PatientEntity.toDomain() = Patient(
    id = id,
    tenantId = tenantId,
    name = name,
    document = document,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status
)

fun Patient.toEntity() = PatientEntity(
    id = id,
    tenantId = tenantId,
    name = name,
    document = document,
    createdAt = createdAt,
    updatedAt = updatedAt,
    status = status
)
