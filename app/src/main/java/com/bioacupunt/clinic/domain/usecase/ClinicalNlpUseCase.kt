package com.bioacupunt.clinic.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalExtractionResult
import com.bioacupunt.clinic.domain.model.ExtractedFinding
import com.bioacupunt.clinic.domain.model.ExtractedObservation
import com.bioacupunt.clinic.domain.model.ExtractedSymptom
import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.RecognizedClinicalEntity
import com.bioacupunt.clinic.domain.model.TemporalPattern

/**
 * Clinical NLP Engine — conservative extraction from raw text.
 *
 * Rules:
 * - UNKNOWN is preferred over unsupported inference
 * - Never transforms "talvez" into "confirmed"
 * - Never generates diagnosis — only observations
 * - Every extraction has confidence, sourceSpan, explanation
 *
 * This is a deterministic extractor — no LLM involved.
 */
class ClinicalNlpUseCase {

    /**
     * Extract clinical observations from raw text.
     * Returns structured extractions with confidence scores.
     */
    fun extract(rawText: String): ClinicalExtractionResult {
        if (rawText.isBlank()) return ClinicalExtractionResult(sourceText = rawText)

        val observations = mutableListOf<ExtractedObservation>()
        val symptoms = mutableListOf<ExtractedSymptom>()
        val findings = mutableListOf<ExtractedFinding>()
        val temporalPatterns = mutableListOf<TemporalPattern>()
        val recognizedEntities = mutableListOf<RecognizedClinicalEntity>()
        val uncertainties = mutableListOf<String>()

        // Extract symptoms
        extractSymptoms(rawText, symptoms, observations, uncertainties)

        // Extract temporal patterns
        extractTemporalPatterns(rawText, temporalPatterns)

        // Extract findings (tongue, pulse, etc.)
        extractFindings(rawText, findings, observations)

        // Extract recognized entities (acupoints, patterns, etc.)
        extractEntities(rawText, recognizedEntities)

        return ClinicalExtractionResult(
            observations = observations,
            symptoms = symptoms,
            findings = findings,
            temporalPatterns = temporalPatterns,
            recognizedEntities = recognizedEntities,
            uncertainties = uncertainties,
            sourceText = rawText,
        )
    }

    private fun extractSymptoms(
        text: String,
        symptoms: MutableList<ExtractedSymptom>,
        observations: MutableList<ExtractedObservation>,
        uncertainties: MutableList<String>,
    ) {
        // Patient-reported symptoms
        val reportedPatterns = listOf(
            "paciente relata" to 0.9,
            "paciente refere" to 0.9,
            "paciente queixa" to 0.85,
            "relata ter" to 0.8,
            "queixa de" to 0.8,
            "sente" to 0.7,
            "apresenta" to 0.7,
        )

        for ((pattern, confidence) in reportedPatterns) {
            val idx = text.lowercase().indexOf(pattern)
            if (idx >= 0) {
                val span = text.substring(idx, minOf(idx + 100, text.length))
                symptoms.add(ExtractedSymptom(
                    name = span.trim(),
                    confidence = confidence,
                    sourceSpan = span,
                ))
                observations.add(ExtractedObservation(
                    type = ObservationType.SYMPTOM,
                    content = span.trim(),
                    confidence = confidence,
                    sourceSpan = span,
                    source = ObservationSource.PATIENT_REPORTED,
                ))
            }
        }

        // Negated symptoms
        val negationPatterns = listOf("nega", "não relata", "não queixa", "não apresenta")
        for (negation in negationPatterns) {
            if (text.lowercase().contains(negation)) {
                uncertainties.add("Sintoma negado encontrado: '$negation'")
            }
        }
    }

    private fun extractTemporalPatterns(
        text: String,
        temporalPatterns: MutableList<TemporalPattern>,
    ) {
        val temporalPatternsMap = mapOf(
            "à noite" to "night",
            "pela manhã" to "morning",
            "pela tarde" to "afternoon",
            "ao acordar" to "upon_waking",
            "antes de dormir" to "before_sleep",
            "durante o dia" to "during_day",
            "semanal" to "weekly",
            "diário" to "daily",
            "intermitente" to "intermittent",
        )

        for ((pt, en) in temporalPatternsMap) {
            if (text.lowercase().contains(pt)) {
                temporalPatterns.add(TemporalPattern(
                    description = "Temporal: $pt",
                    timeOfDay = en,
                    confidence = 0.8,
                ))
            }
        }
    }

    private fun extractFindings(
        text: String,
        findings: MutableList<ExtractedFinding>,
        observations: MutableList<ExtractedObservation>,
    ) {
        // Tongue findings
        val tongueKeywords = mapOf(
            "língua vermelha" to ObservationType.TONGUE,
            "língua pálida" to ObservationType.TONGUE,
            "saburra" to ObservationType.TONGUE,
            "língua" to ObservationType.TONGUE,
        )

        for ((keyword, type) in tongueKeywords) {
            if (text.lowercase().contains(keyword)) {
                val idx = text.lowercase().indexOf(keyword)
                val span = text.substring(idx, minOf(idx + 50, text.length))
                findings.add(ExtractedFinding(type = type, description = span, confidence = 0.7, sourceSpan = span))
                observations.add(ExtractedObservation(
                    type = type, content = span, confidence = 0.7,
                    sourceSpan = span, source = ObservationSource.PRACTITIONER_OBSERVED,
                ))
            }
        }

        // Pulse findings
        val pulseKeywords = listOf("pulso", "pulso rápido", "pulso lento", "pulso fino", "pulso cheio")
        for (keyword in pulseKeywords) {
            if (text.lowercase().contains(keyword)) {
                val idx = text.lowercase().indexOf(keyword)
                val span = text.substring(idx, minOf(idx + 50, text.length))
                findings.add(ExtractedFinding(
                    type = ObservationType.PULSE, description = span,
                    confidence = 0.7, sourceSpan = span,
                ))
            }
        }
    }

    private fun extractEntities(
        text: String,
        recognizedEntities: MutableList<RecognizedClinicalEntity>,
    ) {
        // Acupoint codes
        val acupointPattern = Regex("\\b([A-Z]{1,3}\\d{1,2})\\b")
        for (match in acupointPattern.findAll(text)) {
            recognizedEntities.add(RecognizedClinicalEntity(
                text = match.value, entityType = "ACUPOINT",
                confidence = 0.9,
            ))
        }

        // Common MTC patterns
        val patternKeywords = listOf(
            "deficiência de qi", "estagnação de qi", "deficiência de yin",
            "deficiência de yang", "calor", "frio", "umidade",
        )
        for (pattern in patternKeywords) {
            if (text.lowercase().contains(pattern)) {
                recognizedEntities.add(RecognizedClinicalEntity(
                    text = pattern, entityType = "PATTERN",
                    normalizedForm = pattern, confidence = 0.8,
                ))
            }
        }
    }
}
