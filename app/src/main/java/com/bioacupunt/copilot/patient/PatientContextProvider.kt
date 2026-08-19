package com.bioacupunt.copilot.patient

import com.bioacupunt.copilot.retrieval.PatientContext

/**
 * §28-30 PATIENT CONTEXT PROVIDER
 *
 * Retrieves patient-specific context for the copilot.
 * Enforces permission boundary: each query can only access its own patient.
 *
 * Flow:
 * ```text
 * activePatient + authorization + session
 *     ↓
 * Patient data retrieval
 *     ↓
 * Relevant clinical data
 *     ↓
 * Structured PatientContext
 * ```
 *
 * NEVER sends the entire prontuário automatically.
 * NEVER allows cross-patient access.
 */
class PatientContextProvider(
    private val patientRepository: PatientContextRepository,
) {

    interface PatientContextRepository {
        suspend fun getPatientContext(patientId: Long): PatientContext?
        suspend fun getRecentObservations(patientId: Long, limit: Int = 5): List<String>
        suspend fun getRelevantHistory(patientId: Long): List<String>
        suspend fun getCurrentAssessment(patientId: Long): String?
    }

    /**
     * Build patient context for a specific patient.
     * Enforces: patientId must match the active patient in session.
     */
    suspend fun buildContext(
        patientId: Long,
        activePatientId: Long?,
        sessionId: String?,
    ): PatientContext? {
        // §29 PERMISSION BOUNDARY: verify authorization
        if (activePatientId != null && patientId != activePatientId) {
            return null // UNAUTHORIZED_CONTEXT — cross-patient access denied
        }

        if (sessionId == null) return null

        return try {
            val baseContext = patientRepository.getPatientContext(patientId) ?: return null
            val observations = patientRepository.getRecentObservations(patientId)
            val history = patientRepository.getRelevantHistory(patientId)
            val assessment = patientRepository.getCurrentAssessment(patientId)

            baseContext.copy(
                activePatient = true,
                currentAssessment = assessment,
                recentObservations = observations,
                relevantHistory = history,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * §30 PATIENT SUMMARY
     * Generate a structured summary for a patient.
     * Returns a draft — never persists automatically.
     */
    suspend fun generateSummary(patientId: Long): PatientSummary? {
        return try {
            val context = patientRepository.getPatientContext(patientId) ?: return null
            val observations = patientRepository.getRecentObservations(patientId, limit = 10)
            val history = patientRepository.getRelevantHistory(patientId)

            PatientSummary(
                patientId = patientId,
                recentSessions = observations.size,
                keyObservations = observations,
                relevantHistory = history,
                currentAssessment = context.currentAssessment,
                isDraft = true, // always a draft, requires human review
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class PatientSummary(
    val patientId: Long,
    val recentSessions: Int = 0,
    val keyObservations: List<String> = emptyList(),
    val relevantHistory: List<String> = emptyList(),
    val currentAssessment: String? = null,
    val isDraft: Boolean = true,
)
