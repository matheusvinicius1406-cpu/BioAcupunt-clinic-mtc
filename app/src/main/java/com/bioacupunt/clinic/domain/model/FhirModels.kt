package com.bioacupunt.clinic.domain.model

/**
 * FHIR foundation — mapping layer for interoperability.
 *
 * This is a MAPPING LAYER, not a replacement for internal models.
 * Internal models remain the source of truth; FHIR is for export/import.
 */
data class FhirBundle(
    val resourceType: String = "Bundle",
    val type: String = "collection",
    val entry: List<FhirEntry> = emptyList(),
)

data class FhirEntry(
    val resource: FhirResource,
)

data class FhirResource(
    val resourceType: String,
    val id: String? = null,
    val fields: Map<String, Any> = emptyMap(),
)

/**
 * MTC-specific FHIR extensions.
 * These extend standard FHIR resources with TCM-specific data.
 */
data class MtcFhirObservation(
    val standardObservation: FhirResource,
    val tongueData: Map<String, String>? = null,
    val pulseData: Map<String, String>? = null,
    val patternAssessment: String? = null,
)

data class AcupunctureFhirSession(
    val procedure: FhirResource,
    val acupoints: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val duration: Int? = null,
)

/**
 * FHIR export result.
 */
data class FhirExportResult(
    val bundle: FhirBundle,
    val resourceCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val exportedAt: String = "",
)

/**
 * FHIR import preview — shows what would be imported before confirmation.
 */
data class FhirImportPreview(
    val bundle: FhirBundle,
    val patientPreview: Map<String, String> = emptyMap(),
    val encounterPreview: List<Map<String, String>> = emptyList(),
    val observationPreview: List<Map<String, String>> = emptyList(),
    val conflicts: List<FhirConflict> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class FhirConflict(
    val resourceType: String,
    val resourceId: String,
    val reason: String,
    val localData: Map<String, String> = emptyMap(),
    val remoteData: Map<String, String> = emptyMap(),
)
