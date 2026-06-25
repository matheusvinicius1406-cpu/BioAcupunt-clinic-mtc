package com.example.data.intelligence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val subcategory: String,
    val definition: String,
    val explanation: String,
    val symptoms: String, // Stored as JSON or comma separated string
    val tongueAndPulse: String,
    val energyPatterns: String,
    val commonCauses: String,
    val clinicalEvolution: String,
    val therapeuticApproach: String
)
