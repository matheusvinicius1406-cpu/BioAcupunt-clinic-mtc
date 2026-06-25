package com.example.data.intelligence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "case_results")
data class CaseResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: String,
    val score: Int,
    val date: Long,
    val outcome: String
)
