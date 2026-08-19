package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.ObservationStatus
import com.bioacupunt.clinic.domain.model.ObservationType
import com.bioacupunt.clinic.domain.model.StructuredObservation

interface ObservationRepository {
    suspend fun getById(id: Long): StructuredObservation?
    suspend fun getByEncounterId(encounterId: Long): List<StructuredObservation>
    suspend fun getByPatientId(patientId: Long, limit: Int = 50): List<StructuredObservation>
    suspend fun getByPatientIdAndType(patientId: Long, type: ObservationType): List<StructuredObservation>
    suspend fun getByPatientIdAndStatus(patientId: Long, status: ObservationStatus): List<StructuredObservation>
    suspend fun create(observation: StructuredObservation): Long
    suspend fun update(observation: StructuredObservation)
    suspend fun confirm(id: Long, confirmedBy: String)
    suspend fun reject(id: Long, reviewedBy: String)
    suspend fun delete(id: Long)
}
