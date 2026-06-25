package com.example.data.intelligence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinical_cases")
data class ClinicalCaseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val difficulty: String,
    val patientProfile: String,
    val mainComplaint: String,
    val symptoms: String,
    val tongueDesc: String,
    val pulseDesc: String,
    val correctAnswer: String,
    val explanation: String
)
