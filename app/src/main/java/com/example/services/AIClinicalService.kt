package com.example.services

import com.example.domain.ClinicalInferenceEngine
import com.example.domain.Symptom

class AIClinicalService {
    private val inferenceEngine = ClinicalInferenceEngine()
    
    // In a real implementation, this would call Gemini SDK first to extract symptoms from natural language,
    // then pass the extracted symptom codes to the deterministic inference engine.
    
    suspend fun analyzeConsultationText(text: String): String {
        // 1. Simulating Gemini extracting symptom codes from text
        val extractedSymptoms = mutableListOf<Symptom>()
        if (text.contains("dor no peito", ignoreCase = true)) {
            extractedSymptoms.add(Symptom("S005", "Dor Torácica"))
        }
        if (text.contains("irritada") || text.contains("estressada")) {
            extractedSymptoms.add(Symptom("S001", "Irritabilidade/Dor hipocôndrio"))
        }
        
        if (extractedSymptoms.isEmpty()) {
            return "Nenhum sintoma claro identificado para análise."
        }
        
        // 2. Deterministic Inference (Risk Engine + Rules)
        val result = inferenceEngine.analyzeSymptoms(extractedSymptoms)
        
        if (result.riskFlag) {
            return "ALERTA VERMELHO: Risco clínico identificado (ex: S005). Transição bloqueada. Recomenda-se encaminhamento imediato."
        }
        
        if (!result.valid) {
            return "Análise inconclusiva. Erro: ${result.error}"
        }
        
        // 3. Compile Treatment
        val mainPattern = result.patterns.first()
        val treatment = inferenceEngine.compileTreatment(mainPattern)
        
        return """
            Diagnóstico MTC: ${mainPattern.name} (Confiança: ${mainPattern.confidence})
            Tratamento Recomendado:
            - Pontos: ${treatment.acupoints.joinToString(", ")}
            - Fitoterapia: ${treatment.herbalFormula}
        """.trimIndent()
    }
}
