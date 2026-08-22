package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.TongueObservation

/**
 * Repository for tongue observations.
 * All operations scoped by tenant — no cross-tenant access.
 */
interface TongueObservationRepository {
    suspend fun save(observation: TongueObservation): Result<TongueObservation>
    suspend fun getById(id: Long): Result<TongueObservation?>
    suspend fun getByPatient(patientId: Long): Result<List<TongueObservation>>
    suspend fun getByEncounter(encounterId: Long): Result<List<TongueObservation>>
    suspend fun getLatestConfirmed(patientId: Long): Result<TongueObservation?>
    suspend fun updateStatus(id: Long, status: String): Result<Unit>
    suspend fun markReviewed(id: Long, reviewedBy: String): Result<Unit>
    suspend fun confirm(id: Long, confirmedBy: String): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun countByPatient(patientId: Long): Result<Int>
}
