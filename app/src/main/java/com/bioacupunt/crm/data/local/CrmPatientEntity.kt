package com.bioacupunt.crm.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crm_patients",
    indices = [
        Index("tenantId"),
        Index("name"),
        Index("phone"),
        Index("stage"),
        Index("updatedAt")
    ]
)
data class CrmPatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val birthDate: String = "",
    val stage: String = com.bioacupunt.crm.domain.model.PatientStage.FIRST_CONTACT.name,
    val totalSessions: Int = 0,
    val totalRevenueBrl: Double = 0.0,
    val lastVisit: String = "",
    val nextAppointment: String = "",
    val tags: String = "",
    val notes: String = "",
    val referralSource: String = "",
    val npsScore: Int? = null,
    val healthInsurance: String = "",
    val mainComplaint: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
    val lastModified: String = ""
)
