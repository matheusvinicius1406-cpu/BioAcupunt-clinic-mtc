package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sex: String,
    val profession: String,
    val phone: String,
    val email: String,
    val status: String, // NEW, ACTIVE_EVALUATION, DIAGNOSED, UNDER_TREATMENT, STABLE
    val balance: Double,
    val createdAt: Long
)
