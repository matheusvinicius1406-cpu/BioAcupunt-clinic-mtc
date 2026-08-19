package com.bioacupunt.clinic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questionnaire_responses",
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tenantId"),
        Index("patientId"),
        Index("questionnaireId"),
        Index("status"),
        Index("updatedAt"),
    ],
)
data class QuestionnaireResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val questionnaireId: String,
    val questionnaireVersion: Int,
    val patientId: Long,
    val encounterId: Long? = null,
    val answersJson: String = "{}",
    val status: String,
    val createdAt: String = "",
    val updatedAt: String = "",
)
