package com.example.domain

class ClinicalInferenceEngine {
    // Risk Engine Configuration
    private val highRiskSymptoms = listOf("S005", "S006") // S005: Chest pain, S006: Dyspnea

    fun analyzeSymptoms(symptoms: List<Symptom>): InferenceResult {
        // 1. Risk Evaluation
        val hasRisk = symptoms.any { it.code in highRiskSymptoms }
        if (hasRisk) {
            return InferenceResult(
                valid = false, 
                patterns = emptyList(), 
                error = "E999: Risk Flag Triggered", 
                riskFlag = true
            )
        }

        // 2. Pattern Inference (Deterministic Logic based on MTC rules)
        val patterns = mutableListOf<Pattern>()
        
        // Example Rule: S001 (Hypochondriac pain) -> P002 (Liver Qi Stagnation)
        if (symptoms.any { it.code == "S001" }) {
            patterns.add(Pattern("P002", "Estagnação de Qi do Fígado", 0.85))
        }
        
        // Example Rule: S002 (Night sweats) -> P001 (Yin Deficiency)
        if (symptoms.any { it.code == "S002" }) {
            patterns.add(Pattern("P001", "Deficiência de Yin", 0.90))
        }

        // 3. Confidence Validation
        val validPatterns = patterns.filter { it.confidence >= 0.7 }

        return InferenceResult(
            valid = validPatterns.isNotEmpty(), 
            patterns = validPatterns, 
            error = if (validPatterns.isEmpty()) "E001: No valid patterns found" else null, 
            riskFlag = false
        )
    }

    fun compileTreatment(pattern: Pattern): Treatment {
        return when (pattern.code) {
            "P002" -> Treatment("P002", listOf("F14", "VB34", "TA5", "F3"), "Xiao Yao San")
            "P001" -> Treatment("P001", listOf("R3", "R6", "BP6", "C7"), "Liu Wei Di Huang Wan")
            else -> Treatment(pattern.code, emptyList(), "N/A")
        }
    }
}

data class InferenceResult(
    val valid: Boolean,
    val patterns: List<Pattern>,
    val error: String?,
    val riskFlag: Boolean
)
