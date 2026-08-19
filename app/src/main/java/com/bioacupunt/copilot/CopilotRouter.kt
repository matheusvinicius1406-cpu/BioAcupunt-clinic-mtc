package com.bioacupunt.copilot

import com.bioacupunt.copilot.retrieval.IntentType

/**
 * §35 COPILOT ROUTER
 *
 * Maps detected intent to the appropriate tool/use case.
 * Each route declares its requirements (patient context, authentication, etc.).
 *
 * Mappings:
 * - "resuma a evolução" → PatientSummary
 * - "por que A está acima de B?" → DifferentialExplanation
 * - "o que falta?" → MissingData
 * - "qual a fonte?" → EvidenceExplorer
 * - "pesquise insônia" → KnowledgeSearch
 * - "quais pontos estão relacionados?" → PointLookup
 */
class CopilotRouter {

    data class CopilotRoute(
        val tool: CopilotTool,
        val requiresPatientContext: Boolean = false,
        val requiresAuthentication: Boolean = true,
        val readOnly: Boolean = true,
        val allowedOffline: Boolean = true,
    )

    /**
     * Route an intent to the appropriate tool.
     * Deterministic: same intent → same route, always.
     */
    fun route(intent: IntentType): CopilotRoute {
        return when (intent) {
            IntentType.PATIENT_SUMMARY -> CopilotRoute(
                tool = CopilotTool.PATIENT_SUMMARY,
                requiresPatientContext = true,
            )
            IntentType.DIFFERENTIAL_EXPLANATION -> CopilotRoute(
                tool = CopilotTool.DIFFERENTIAL_EXPLANATION,
                requiresPatientContext = true,
            )
            IntentType.MISSING_DATA -> CopilotRoute(
                tool = CopilotTool.MISSING_DATA,
                requiresPatientContext = true,
            )
            IntentType.EVIDENCE_LOOKUP -> CopilotRoute(
                tool = CopilotTool.EVIDENCE_LOOKUP,
            )
            IntentType.POINT_LOOKUP -> CopilotRoute(
                tool = CopilotTool.POINT_LOOKUP,
            )
            IntentType.FORMULA_LOOKUP -> CopilotRoute(
                tool = CopilotTool.FORMULA_LOOKUP,
            )
            IntentType.PROTOCOL_LOOKUP -> CopilotRoute(
                tool = CopilotTool.PROTOCOL_LOOKUP,
            )
            IntentType.KNOWLEDGE_SEARCH -> CopilotRoute(
                tool = CopilotTool.KNOWLEDGE_SEARCH,
            )
            IntentType.CLINICAL_ANALYSIS -> CopilotRoute(
                tool = CopilotTool.KNOWLEDGE_SEARCH,
                requiresPatientContext = true,
            )
            IntentType.RESEARCH_QUERY -> CopilotRoute(
                tool = CopilotTool.KNOWLEDGE_SEARCH,
            )
            IntentType.GENERAL_CLINICAL_QUERY -> CopilotRoute(
                tool = CopilotTool.KNOWLEDGE_SEARCH,
            )
        }
    }
}

enum class CopilotTool {
    KNOWLEDGE_SEARCH,
    PATIENT_SUMMARY,
    DIFFERENTIAL_EXPLANATION,
    MISSING_DATA,
    EVIDENCE_LOOKUP,
    POINT_LOOKUP,
    FORMULA_LOOKUP,
    PROTOCOL_LOOKUP,
}
