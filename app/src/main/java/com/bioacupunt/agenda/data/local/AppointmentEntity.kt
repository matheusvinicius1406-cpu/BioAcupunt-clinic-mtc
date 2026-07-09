package com.bioacupunt.agenda.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("date"),
        Index("status"),
        Index("patientId", "date"),
        Index("updatedAt")
    ],
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val patientId: Long,
    val patientName: String,
    val date: String,
    val time: String,
    val durationMin: Int = 60,
    val type: String = com.bioacupunt.agenda.domain.model.AppointmentType.ACUPUNCTURE.name,
    val status: String = com.bioacupunt.agenda.domain.model.AppointmentStatus.SCHEDULED.name,
    val notes: String = "",
    val sessionNumber: Int = 1,
    val reminderMinutesBefore: Int = 30,
    val valueBrl: Double = 0.0,
    val paid: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
    val lastModified: String = ""
)
