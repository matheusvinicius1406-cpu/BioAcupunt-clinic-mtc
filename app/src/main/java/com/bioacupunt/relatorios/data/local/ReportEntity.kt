package com.bioacupunt.relatorios.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bioacupunt.relatorios.domain.model.ReportStatus

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val body: String = "",
    val filtersJson: String = "{}",
    val generatedAt: String = "",
    val patientId: Long? = null,
    val status: String = ReportStatus.DRAFT.name
)
