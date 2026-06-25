package com.example.domain

enum class ClinicalState {
    NEW, ACTIVE_EVALUATION, DIAGNOSED, UNDER_TREATMENT, STABLE
}

data class Patient(
    val id: String,
    val name: String,
    val state: ClinicalState = ClinicalState.NEW,
    val riskFlag: Boolean = false
)

data class Symptom(val code: String, val description: String)
data class Sign(val code: String, val description: String)
data class Pattern(val code: String, val name: String, val confidence: Double)

data class Treatment(
    val patternCode: String,
    val acupoints: List<String>,
    val herbalFormula: String
)
