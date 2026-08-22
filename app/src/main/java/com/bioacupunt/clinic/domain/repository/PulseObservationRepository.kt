package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.PulseObservation

/**
 * Repository for pulse observations.
 * All operations scoped by tenant — no cross-tenant access.
 */
interface PulseObservationRepository {
    suspend fun save(observation: PulseObservation): Result<PulseObservation>
    suspend fun getById(id: Long): Result<PulseObservation?>
    suspend fun getByPatient(patientId: Long): Result<List<PulseObservation>>
    suspend fun getByEncounter(encounterId: Long): Result<List<PulseObservation>>
    suspend fun getLatestConfirmed(patientId: Long): Result<PulseObservation?>
    suspend fun updateStatus(id: Long, status: String): Result<Unit>
    suspend fun markReviewed(id: Long, reviewedBy: String): Result<Unit>
    suspend fun confirm(id: Long, confirmedBy: String): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun countByPatient(patientId: Long): Result<Int>
}
