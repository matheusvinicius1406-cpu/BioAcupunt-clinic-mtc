package com.bioacupunt.clinic

import com.bioacupunt.clinic.domain.model.ObservationSource
import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation
import com.bioacupunt.clinic.domain.usecase.CompareClinicalSessionsUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionComparisonTest {

    private lateinit var useCase: CompareClinicalSessionsUseCase

    @Before
    fun setup() {
        useCase = CompareClinicalSessionsUseCase()
    }

    @Test
    fun compare_sameObservations_persistent() {
        val obsA = listOf(
            makeObs("Insônia", ObservationType.SLEEP),
            makeObs("Dor lombar", ObservationType.PAIN),
        )
        val obsB = listOf(
            makeObs("Insônia", ObservationType.SLEEP),
            makeObs("Dor lombar", ObservationType.PAIN),
        )

        val result = useCase.compare(obsA, obsB)
        assertEquals(2, result.persistentFindings.size)
        assertTrue(result.newFindings.isEmpty())
        assertTrue(result.resolvedFindings.isEmpty())
    }

    @Test
    fun compare_newFinding() {
        val obsA = listOf(makeObs("Insônia", ObservationType.SLEEP))
        val obsB = listOf(
            makeObs("Insônia", ObservationType.SLEEP),
            makeObs("Náusea", ObservationType.DIGESTIVE),
        )

        val result = useCase.compare(obsA, obsB)
        assertTrue("Should detect new finding", result.newFindings.any { it.contains("náusea") })
    }

    @Test
    fun compare_resolvedFinding() {
        val obsA = listOf(
            makeObs("Insônia", ObservationType.SLEEP),
            makeObs("Náusea", ObservationType.DIGESTIVE),
        )
        val obsB = listOf(makeObs("Insônia", ObservationType.SLEEP))

        val result = useCase.compare(obsA, obsB)
        assertTrue("Should detect resolved finding", result.resolvedFindings.any { it.contains("náusea") })
    }

    @Test
    fun compare_emptySessions_noChanges() {
        val result = useCase.compare(emptyList(), emptyList())
        assertTrue(result.newFindings.isEmpty())
        assertTrue(result.resolvedFindings.isEmpty())
        assertTrue(result.persistentFindings.isEmpty())
    }

    @Test
    fun compare_allNew() {
        val obsA = emptyList<StructuredObservation>()
        val obsB = listOf(makeObs("Cefaleia", ObservationType.SYMPTOM))

        val result = useCase.compare(obsA, obsB)
        assertEquals(1, result.newFindings.size)
    }

    @Test
    fun compare_allResolved() {
        val obsA = listOf(makeObs("Cefaleia", ObservationType.SYMPTOM))
        val obsB = emptyList<StructuredObservation>()

        val result = useCase.compare(obsA, obsB)
        assertEquals(1, result.resolvedFindings.size)
    }

    private fun makeObs(content: String, type: ObservationType) = StructuredObservation(
        id = 0,
        tenantId = 1,
        encounterId = 1,
        patientId = 1,
        type = type,
        content = content,
        status = ObservationStatus.CONFIRMED,
        source = ObservationSource.PATIENT_REPORTED,
        createdAt = "2026-01-01",
    )
}
