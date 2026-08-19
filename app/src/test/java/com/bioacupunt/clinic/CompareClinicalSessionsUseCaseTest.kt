package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.usecase.CompareClinicalSessionsUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CompareClinicalSessionsUseCaseTest {

    private lateinit var useCase: CompareClinicalSessionsUseCase

    @Before
    fun setup() {
        useCase = CompareClinicalSessionsUseCase()
    }

    @Test
    fun compare_identicalSessions_noDifferences() {
        val obs = listOf(
            makeObs("Dor lombar"),
            makeObs("Insônia"),
        )
        val result = useCase.compare(obs, obs, 1, 2)
        assertTrue(result.newFindings.isEmpty())
        assertTrue(result.resolvedFindings.isEmpty())
        assertTrue(result.persistentFindings.isNotEmpty())
    }

    @Test
    fun compare_newFindings_detected() {
        val sessionA = listOf(makeObs("Dor lombar"))
        val sessionB = listOf(makeObs("Dor lombar"), makeObs("Cefaleia"))
        val result = useCase.compare(sessionA, sessionB, 1, 2)
        assertTrue(result.newFindings.any { it.contains("cefaleia") })
    }

    @Test
    fun compare_resolvedFindings_detected() {
        val sessionA = listOf(makeObs("Dor lombar"), makeObs("Insônia"))
        val sessionB = listOf(makeObs("Dor lombar"))
        val result = useCase.compare(sessionA, sessionB, 1, 2)
        assertTrue(result.resolvedFindings.any { it.contains("insônia") })
    }

    @Test
    fun compare_persistentFindings_detected() {
        val sessionA = listOf(makeObs("Dor lombar"))
        val sessionB = listOf(makeObs("Dor lombar"))
        val result = useCase.compare(sessionA, sessionB, 1, 2)
        assertTrue(result.persistentFindings.any { it.contains("dor lombar") })
    }

    @Test
    fun compare_emptySessions_noDifferences() {
        val result = useCase.compare(emptyList(), emptyList(), 1, 2)
        assertTrue(result.newFindings.isEmpty())
        assertTrue(result.resolvedFindings.isEmpty())
    }

    @Test
    fun compare_deterministic_sameInputSameOutput() {
        val sessionA = listOf(makeObs("Dor lombar"))
        val sessionB = listOf(makeObs("Cefaleia"))
        val r1 = useCase.compare(sessionA, sessionB, 1, 2)
        val r2 = useCase.compare(sessionA, sessionB, 1, 2)
        assertEquals(r1.newFindings, r2.newFindings)
        assertEquals(r1.resolvedFindings, r2.resolvedFindings)
    }

    private fun makeObs(content: String) = StructuredObservation(
        id = 0,
        tenantId = 1,
        encounterId = 1,
        patientId = 1,
        type = ObservationType.SYMPTOM,
        content = content,
        status = ObservationStatus.CONFIRMED,
        source = ObservationSource.PATIENT_REPORTED,
        createdAt = "2026-01-01",
    )
}
