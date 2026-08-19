package com.bioacupunt.clinic.data.local

import com.bioacupunt.clinic.domain.model.ClinicalNote
import com.bioacupunt.clinic.domain.model.Encounter
import com.bioacupunt.clinic.domain.model.EncounterStatus
import com.bioacupunt.clinic.domain.model.EncounterType
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.FollowUpStatus
import com.bioacupunt.clinic.domain.model.NoteFormat
import com.bioacupunt.clinic.domain.model.NoteStatus
import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.QuestionnaireResponse
import com.bioacupunt.clinic.domain.model.ResponseStatus
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.model.TreatmentCategory
import com.bioacupunt.clinic.domain.model.TreatmentPlan
import com.bioacupunt.clinic.domain.model.TreatmentPlanItem
import com.bioacupunt.clinic.domain.model.TreatmentPlanStatus
import com.bioacupunt.core.util.AppJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

private fun encodeMap(values: Map<String, String>): String =
    JsonObject(values.mapValues { JsonPrimitive(it.value) }).toString()

private fun decodeMap(json: String): Map<String, String> =
    runCatching {
        AppJson.parseToJsonElement(json).jsonObject.mapValues { it.value.jsonPrimitive.content }
    }.getOrDefault(emptyMap())

private fun encodeStringList(values: List<String>): String =
    JsonArray(values.map { JsonPrimitive(it) }).toString()

private fun decodeStringList(json: String): List<String> =
    runCatching {
        AppJson.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
    }.getOrDefault(emptyList())

private fun encodeTreatmentItems(items: List<TreatmentPlanItem>): String =
    JsonArray(items.map { item ->
        JsonObject(
            mapOf(
                "id" to JsonPrimitive(item.id),
                "category" to JsonPrimitive(item.category.name),
                "description" to JsonPrimitive(item.description),
                "details" to JsonPrimitive(item.details),
                "isAiSuggested" to JsonPrimitive(item.isAiSuggested),
                "isConfirmed" to JsonPrimitive(item.isConfirmed),
            )
        )
    }).toString()

private fun decodeTreatmentItems(json: String): List<TreatmentPlanItem> =
    runCatching {
        AppJson.parseToJsonElement(json).jsonArray.map { element ->
            val obj = element.jsonObject
            TreatmentPlanItem(
                id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                category = enumValue(obj["category"]?.jsonPrimitive?.contentOrNull.orEmpty(), TreatmentCategory.OTHER),
                description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                details = obj["details"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isAiSuggested = obj["isAiSuggested"]?.jsonPrimitive?.booleanOrNull ?: false,
                isConfirmed = obj["isConfirmed"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }
    }.getOrDefault(emptyList())

fun EncounterEntity.toDomain(): Encounter = Encounter(
    id = id,
    tenantId = tenantId,
    patientId = patientId,
    status = enumValue(status, EncounterStatus.PLANNED),
    type = enumValue(type, EncounterType.CONSULTATION),
    startedAt = startedAt,
    endedAt = endedAt,
    practitionerId = practitionerId,
    reason = reason,
    appointmentId = appointmentId,
    currentAssessmentId = currentAssessmentId,
    currentNoteId = currentNoteId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Encounter.toEntity(now: String = updatedAt.ifBlank { createdAt }): EncounterEntity = EncounterEntity(
    id = id,
    tenantId = tenantId,
    patientId = patientId,
    status = status.name,
    type = type.name,
    startedAt = startedAt,
    endedAt = endedAt,
    practitionerId = practitionerId,
    reason = reason,
    appointmentId = appointmentId,
    currentAssessmentId = currentAssessmentId,
    currentNoteId = currentNoteId,
    createdAt = createdAt.ifBlank { now },
    updatedAt = now,
    deleted = deletedAt != null,
)

fun StructuredObservationEntity.toDomain(): StructuredObservation = StructuredObservation(
    id = id,
    tenantId = tenantId,
    encounterId = encounterId,
    patientId = patientId,
    type = enumValue(type, ObservationType.GENERAL),
    content = content,
    structuredData = decodeMap(structuredDataJson),
    status = enumValue(status, ObservationStatus.DRAFT),
    source = enumValue(source, ObservationSource.MANUAL_ENTRY),
    sourceSpan = sourceSpan,
    confidence = confidence,
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt,
    confirmedBy = confirmedBy,
    confirmedAt = confirmedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun StructuredObservation.toEntity(now: String = updatedAt.ifBlank { createdAt }): StructuredObservationEntity =
    StructuredObservationEntity(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        type = type.name,
        content = content,
        structuredDataJson = encodeMap(structuredData),
        status = status.name,
        source = source.name,
        sourceSpan = sourceSpan,
        confidence = confidence,
        reviewedBy = reviewedBy,
        reviewedAt = reviewedAt,
        confirmedBy = confirmedBy,
        confirmedAt = confirmedAt,
        createdAt = createdAt.ifBlank { now },
        updatedAt = now,
        deleted = deletedAt != null,
    )

fun ClinicalNoteEntity.toDomain(): ClinicalNote = ClinicalNote(
    id = id,
    tenantId = tenantId,
    encounterId = encounterId,
    patientId = patientId,
    format = enumValue(format, NoteFormat.SOAP),
    subjective = subjective,
    objective = objective,
    assessment = assessment,
    plan = plan,
    mtcAssessmentSummary = mtcAssessmentSummary,
    references = decodeStringList(referencesJson),
    status = enumValue(status, NoteStatus.DRAFT),
    createdBy = createdBy,
    finalizedBy = finalizedBy,
    finalizedAt = finalizedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ClinicalNote.toEntity(now: String = updatedAt.ifBlank { createdAt }): ClinicalNoteEntity =
    ClinicalNoteEntity(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        format = format.name,
        subjective = subjective,
        objective = objective,
        assessment = assessment,
        plan = plan,
        mtcAssessmentSummary = mtcAssessmentSummary,
        referencesJson = encodeStringList(references),
        status = status.name,
        createdBy = createdBy,
        finalizedBy = finalizedBy,
        finalizedAt = finalizedAt,
        createdAt = createdAt.ifBlank { now },
        updatedAt = now,
        deleted = deletedAt != null,
    )

fun TreatmentPlanEntity.toDomain(): TreatmentPlan = TreatmentPlan(
    id = id,
    tenantId = tenantId,
    encounterId = encounterId,
    patientId = patientId,
    goals = goals,
    principles = principles,
    items = decodeTreatmentItems(itemsJson),
    frequency = frequency,
    duration = duration,
    followUpRecommendation = followUpRecommendation,
    status = enumValue(status, TreatmentPlanStatus.DRAFT),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TreatmentPlan.toEntity(now: String = updatedAt.ifBlank { createdAt }): TreatmentPlanEntity =
    TreatmentPlanEntity(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        goals = goals,
        principles = principles,
        itemsJson = encodeTreatmentItems(items),
        frequency = frequency,
        duration = duration,
        followUpRecommendation = followUpRecommendation,
        status = status.name,
        createdAt = createdAt.ifBlank { now },
        updatedAt = now,
        deleted = deletedAt != null,
    )

fun FollowUpEntity.toDomain(): FollowUp = FollowUp(
    id = id,
    tenantId = tenantId,
    patientId = patientId,
    encounterId = encounterId,
    scheduledAt = scheduledAt,
    reason = reason,
    expectedFindings = expectedFindings,
    actualFindings = actualFindings,
    status = enumValue(status, FollowUpStatus.SCHEDULED),
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FollowUp.toEntity(now: String = updatedAt.ifBlank { createdAt }): FollowUpEntity = FollowUpEntity(
    id = id,
    tenantId = tenantId,
    patientId = patientId,
    encounterId = encounterId,
    scheduledAt = scheduledAt,
    reason = reason,
    expectedFindings = expectedFindings,
    actualFindings = actualFindings,
    status = status.name,
    completedAt = completedAt,
    createdAt = createdAt.ifBlank { now },
    updatedAt = now,
    deleted = deletedAt != null,
)

fun QuestionnaireResponseEntity.toDomain(): QuestionnaireResponse = QuestionnaireResponse(
    id = id,
    tenantId = tenantId,
    questionnaireId = questionnaireId,
    questionnaireVersion = questionnaireVersion,
    patientId = patientId,
    encounterId = encounterId,
    answers = decodeMap(answersJson),
    status = enumValue(status, ResponseStatus.IN_PROGRESS),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun QuestionnaireResponse.toEntity(now: String = updatedAt.ifBlank { createdAt }): QuestionnaireResponseEntity =
    QuestionnaireResponseEntity(
        id = id,
        tenantId = tenantId,
        questionnaireId = questionnaireId,
        questionnaireVersion = questionnaireVersion,
        patientId = patientId,
        encounterId = encounterId,
        answersJson = encodeMap(answers),
        status = status.name,
        createdAt = createdAt.ifBlank { now },
        updatedAt = now,
    )
