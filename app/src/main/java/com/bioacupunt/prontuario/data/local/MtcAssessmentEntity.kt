package com.bioacupunt.prontuario.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence for [com.bioacupunt.prontuario.domain.model.MtcAssessment].
 *
 * Design note — why JSON columns for the nested findings:
 *
 * Tongue, pulse, patterns and body marks are *value objects owned by the
 * assessment*. They are never queried independently, never joined, and never
 * updated in isolation — they are read and written as a unit, with the assessment.
 * Normalising them into four child tables would buy nothing and cost four joins on
 * every read, plus four migrations every time the TCM model grows (and it will).
 *
 * What is NOT hidden in JSON: the columns the app actually filters and reports on
 * (`patientId`, `date`, `flagsCsv`). Clinical flags are a CSV column precisely
 * because the Safety Engine and Analytics must be able to query them — burying a
 * contraindication inside a JSON blob would make it invisible to SQL, which is
 * exactly the kind of shortcut that gets a patient hurt.
 */
@Entity(
    tableName = "mtc_assessments",
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.patient.data.local.PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patientId"), Index("date"), Index("updatedAt")],
)
data class MtcAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val date: String = "",
    val chiefComplaint: String = "",

    // Ba Gang — flat, cheap to query for analytics ("how many Interior/Cold cases?")
    val baGangPolarity: String = "UNSET",
    val baGangDepth: String = "UNSET",
    val baGangTemperature: String = "UNSET",
    val baGangStrength: String = "UNSET",

    // Owned value objects, serialised as a unit.
    val patternsJson: String = "[]",
    val tongueJson: String = "{}",
    val pulseJson: String = "{}",
    val bodyMarksJson: String = "[]",

    /** Queryable. Never a JSON blob — the Safety Engine depends on this. */
    val flagsCsv: String = "",
    val gestationalWeeks: Int? = null,

    val clinicalImpression: String = "",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)
