package com.bioacupunt.clinic.domain.model

/**
 * A structured questionnaire for clinical data collection.
 *
 * Supports:
 * - Multiple sections
 * - Conditional logic (if answer == X → show Y)
 * - Versioning
 * - Questionnaire → Observation mapping
 */
data class Questionnaire(
    val id: String,
    val version: Int = 1,
    val title: String,
    val description: String = "",
    val sections: List<QuestionnaireSection> = emptyList(),
    val status: QuestionnaireStatus = QuestionnaireStatus.DRAFT,
    val createdAt: String = "",
    val updatedAt: String = "",
)

enum class QuestionnaireStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED,
}

data class QuestionnaireSection(
    val id: String,
    val title: String,
    val items: List<QuestionnaireItem> = emptyList(),
    /** Conditional: show this section only if condition is met */
    val condition: QuestionnaireCondition? = null,
)

data class QuestionnaireItem(
    val id: String,
    val type: QuestionnaireItemType,
    val label: String,
    val description: String = "",
    val required: Boolean = false,
    val options: List<QuestionnaireOption> = emptyList(),
    /** Conditional: show this item only if condition is met */
    val condition: QuestionnaireCondition? = null,
    /** Mapping: this answer maps to a specific ObservationType */
    val observationMapping: ObservationType? = null,
    val validation: QuestionnaireValidation? = null,
)

enum class QuestionnaireItemType {
    TEXT,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    DATE,
    TIME,
    QUANTITY,
}

data class QuestionnaireOption(
    val id: String,
    val label: String,
    val value: String,
)

data class QuestionnaireCondition(
    val dependsOnItemId: String,
    val operator: ConditionOperator,
    val value: String,
)

enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN,
}

data class QuestionnaireValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val pattern: String? = null,
)

/**
 * A response to a questionnaire.
 */
data class QuestionnaireResponse(
    val id: Long = 0,
    val tenantId: Long,
    val questionnaireId: String,
    val questionnaireVersion: Int,
    val patientId: Long,
    val encounterId: Long? = null,
    val answers: Map<String, String> = emptyMap(),
    val status: ResponseStatus = ResponseStatus.IN_PROGRESS,
    val createdAt: String = "",
    val updatedAt: String = "",
)

enum class ResponseStatus {
    IN_PROGRESS,
    COMPLETED,
}
