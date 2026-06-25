package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val appointmentTime: Long,
    val duration: Int,
    val status: String, // scheduled, completed, etc.
    val treatmentType: String,
    val notes: String
)
