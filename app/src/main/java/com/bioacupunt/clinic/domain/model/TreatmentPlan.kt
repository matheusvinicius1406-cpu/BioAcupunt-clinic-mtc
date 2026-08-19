package com.bioacupunt.clinic.domain.model

/**
 * A treatment plan for a patient, associated with an encounter.
 *
 * AI can suggest treatment plan items, but the professional must CONFIRM.
 * Nothing is auto-persisted as final.
 */
data class TreatmentPlan(
    val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val goals: String = "",
    val principles: String = "",
    val items: List<TreatmentPlanItem> = emptyList(),
    val frequency: String = "",
    val duration: String = "",
    val followUpRecommendation: String = "",
    val status: TreatmentPlanStatus = TreatmentPlanStatus.DRAFT,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

data class TreatmentPlanItem(
    val id: String = "",
    val category: TreatmentCategory = TreatmentCategory.ACUPUNCTURE,
    val description: String = "",
    val details: String = "",
    val isAiSuggested: Boolean = false,
    val isConfirmed: Boolean = false,
)

enum class TreatmentCategory(val label: String) {
    ACUPUNCTURE("Acupuntura"),
    HERBAL("Fitoterapia"),
    DIETARY("Alimentação"),
    LIFESTYLE("Estilo de vida"),
    EXERCISE("Exercício"),
    MOXIBUSTION("Moxabustão"),
    CUPPING("Ventosaterapia"),
    OTHER("Outro"),
}

enum class TreatmentPlanStatus {
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
}
