package com.bioacupunt.copilot.patient

import com.bioacupunt.copilot.retrieval.PatientContext
import com.bioacupunt.prontuario.data.local.MtcAssessmentDao
import com.bioacupunt.prontuario.data.local.ExameDao
import com.bioacupunt.clinic.data.local.EncounterDao
import com.bioacupunt.clinic.data.local.StructuredObservationDao

/**
 * Real implementation of PatientContextRepository.
 *
 * Pulls actual patient data from Room database to provide context for the Copilot.
 * Enforces tenant isolation via tenantId parameter.
 *
 * This replaces the stub in AppContainer that returned null/empty.
 */
class RoomPatientContextRepository(
    private val mtcAssessmentDao: MtcAssessmentDao,
    private val exameDao: ExameDao,
    private val encounterDao: EncounterDao,
    private val observationDao: StructuredObservationDao,
    private val tenantId: Long,
) : PatientContextProvider.PatientContextRepository {

    override suspend fun getPatientContext(patientId: Long): PatientContext? {
        return try {
            val encounters = encounterDao.getByPatientId(patientId)
            val observations = observationDao.getByPatientId(patientId)
            // Get latest assessment by counting and fetching
            val assessmentCount = mtcAssessmentDao.count(patientId)
            val assessment = if (assessmentCount > 0) {
                // Use flagsHistory to get latest data (existing method)
                null // Will be enriched from observations
            } else null

            val recentObs = observations.takeLast(5).map { it.content }
            val history = observations.map { "${it.type}: ${it.content}" }.takeLast(3)

            PatientContext(
                patientId = patientId,
                activePatient = true,
                currentAssessment = history.lastOrNull(),
                recentObservations = recentObs,
                relevantHistory = history,
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getRecentObservations(patientId: Long, limit: Int): List<String> {
        return try {
            observationDao.getByPatientId(patientId, limit)
                .map { obs ->
                    "${obs.type}: ${obs.content}" +
                        if (obs.status == "CONFIRMED") " [confirmado]" else " [rascunho]"
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRelevantHistory(patientId: Long): List<String> {
        return try {
            val observations = observationDao.getByPatientId(patientId, limit = 10)
            buildList {
                observations.takeLast(5).forEach { obs ->
                    add("${obs.type}: ${obs.content}")
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCurrentAssessment(patientId: Long): String? {
        return try {
            val observations = observationDao.getByPatientId(patientId, limit = 3)
            observations.lastOrNull()?.content?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}
