package com.bioacupunt.mtc.knowledge.domain

import com.bioacupunt.prontuario.domain.model.MtcAssessment

/**
 * UseCase for running Clinical Intelligence on a patient assessment.
 *
 * Architecture:
 * UI → UseCase → ClinicalIntelligenceEngine → Knowledge Core
 *
 * This is a Clinical Decision Support tool — the result is a structured suggestion
 * that the doctor reviews. It never saves automatically.
 *
 * R1/R2/R4 intact.
 */
class RunClinicalIntelligenceUseCase(
    private val engine: ClinicalIntelligenceEngine,
) {

    /**
     * Analyze a patient assessment and return structured clinical intelligence.
     *
     * @param assessment The current MtcAssessment from the prontuário
     * @return ClinicalIntelligenceResult with ranked hypotheses, evidence, and missing data
     */
    suspend operator fun invoke(assessment: MtcAssessment): ClinicalIntelligenceResult {
        val observation = mapAssessmentToObservation(assessment)
        return engine.analyze(observation)
    }

    /**
     * Map an MtcAssessment to a ClinicalObservation.
     * Only maps data that actually exists — never infers.
     */
    private fun mapAssessmentToObservation(assessment: MtcAssessment): ClinicalObservation {
        val symptoms = mutableListOf<String>()
        val tongueFindings = mutableListOf<String>()
        val pulseFindings = mutableListOf<String>()
        val zangFuPatterns = mutableListOf<String>()

        // Chief complaint as primary symptom
        if (assessment.chiefComplaint.isNotBlank()) {
            symptoms.add(assessment.chiefComplaint)
        }

        // Review of systems as additional symptoms
        symptoms.addAll(assessment.reviewOfSystems)

        // Tongue findings
        with(assessment.tongue) {
            if (bodyColor != com.bioacupunt.prontuario.domain.model.TongueBodyColor.UNSET) {
                tongueFindings.add("língua ${bodyColor.label}")
            }
            if (coatingColor != com.bioacupunt.prontuario.domain.model.TongueCoatingColor.UNSET) {
                tongueFindings.add("saburra ${coatingColor.label}")
            }
            if (coatingThickness != com.bioacupunt.prontuario.domain.model.TongueCoatingThickness.UNSET) {
                tongueFindings.add("saburra ${coatingThickness.label}")
            }
            if (moisture != com.bioacupunt.prontuario.domain.model.TongueMoisture.UNSET) {
                tongueFindings.add("umidade ${moisture.label}")
            }
            if (shapes.isNotEmpty()) {
                tongueFindings.addAll(shapes.map { "forma ${it.label}" })
            }
            if (notes.isNotBlank()) {
                tongueFindings.add(notes)
            }
        }

        // Pulse findings
        with(assessment.pulse) {
            if (allQualities.isNotEmpty()) {
                pulseFindings.addAll(allQualities.map { it.label })
            }
            if (notes.isNotBlank()) {
                pulseFindings.add(notes)
            }
        }

        // Zang-Fu patterns
        assessment.patterns.forEach { pattern ->
            zangFuPatterns.add("${pattern.organ.label}: ${pattern.factors.joinToString { it.label }}")
        }

        // Ba Gang
        val baGang = with(assessment.baGang) {
            BaGangData(
                polarity = when (polarity) {
                    com.bioacupunt.prontuario.domain.model.BaGangPolarity.YIN -> "YIN"
                    com.bioacupunt.prontuario.domain.model.BaGangPolarity.YANG -> "YANG"
                    else -> null
                },
                depth = when (depth) {
                    com.bioacupunt.prontuario.domain.model.BaGangDepth.EXTERIOR -> "EXTERIOR"
                    com.bioacupunt.prontuario.domain.model.BaGangDepth.INTERIOR -> "INTERIOR"
                    else -> null
                },
                temperature = when (temperature) {
                    com.bioacupunt.prontuario.domain.model.BaGangTemperature.COLD -> "COLD"
                    com.bioacupunt.prontuario.domain.model.BaGangTemperature.HEAT -> "HEAT"
                    else -> null
                },
                strength = when (strength) {
                    com.bioacupunt.prontuario.domain.model.BaGangStrength.DEFICIENCY -> "DEFICIENCY"
                    com.bioacupunt.prontuario.domain.model.BaGangStrength.EXCESS -> "EXCESS"
                    else -> null
                },
            )
        }

        // Aggravating/relieving factors as context
        val context = mutableMapOf<String, String>()
        if (assessment.aggravatingFactors.isNotEmpty()) {
            context["aggravating"] = assessment.aggravatingFactors.joinToString(", ")
        }
        if (assessment.relievingFactors.isNotEmpty()) {
            context["relieving"] = assessment.relievingFactors.joinToString(", ")
        }

        return ClinicalObservation(
            symptoms = symptoms,
            tongueFindings = tongueFindings,
            pulseFindings = pulseFindings,
            zangFuPatterns = zangFuPatterns,
            baGang = baGang,
            context = context,
        )
    }
}
