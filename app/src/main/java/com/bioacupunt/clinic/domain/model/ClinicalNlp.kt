package com.bioacupunt.clinic.domain.model

/**
 * Result of Clinical NLP extraction from raw text.
 *
 * Conservative by design: UNKNOWN is preferred over unsupported inference.
 * The NLP never transforms "talvez" into "confirmed".
 * It never generates diagnosis — only observations.
 */
data class ClinicalExtractionResult(
    val observations: List<ExtractedObservation> = emptyList(),
    val symptoms: List<ExtractedSymptom> = emptyList(),
    val findings: List<ExtractedFinding> = emptyList(),
    val temporalPatterns: List<TemporalPattern> = emptyList(),
    val recognizedEntities: List<RecognizedClinicalEntity> = emptyList(),
    val uncertainties: List<String> = emptyList(),
    val sourceText: String = "",
)

data class ExtractedObservation(
    val type: ObservationType,
    val content: String,
    val confidence: Double,
    val sourceSpan: String? = null,
    val explanation: String? = null,
    val status: ObservationStatus = ObservationStatus.DRAFT,
    val source: ObservationSource = ObservationSource.AI_EXTRACTED_DRAFT,
)

data class ExtractedSymptom(
    val name: String,
    val severity: String? = null,
    val duration: String? = null,
    val location: String? = null,
    val confidence: Double,
    val sourceSpan: String? = null,
)

data class ExtractedFinding(
    val type: ObservationType,
    val description: String,
    val confidence: Double,
    val sourceSpan: String? = null,
)

data class TemporalPattern(
    val description: String,
    val timeOfDay: String? = null,
    val frequency: String? = null,
    val relation: String? = null, // "piora à noite", "melhora com calor"
    val confidence: Double,
)

data class RecognizedClinicalEntity(
    val text: String,
    val entityType: String,
    val normalizedForm: String? = null,
    val confidence: Double,
)
