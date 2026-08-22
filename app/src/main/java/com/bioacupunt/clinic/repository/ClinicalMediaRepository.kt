package com.bioacupunt.clinic.repository

import com.bioacupunt.clinic.domain.model.ClinicalMedia
import com.bioacupunt.clinic.domain.model.ClinicalMediaStatus

/**
 * Repository for clinical media — images, audio, video, documents.
 *
 * Binary content is stored in secure app-internal storage.
 * This repository manages metadata only, with full tenant isolation.
 */
interface ClinicalMediaRepository {

    suspend fun save(media: ClinicalMedia): Result<ClinicalMedia>

    suspend fun getById(id: Long): Result<ClinicalMedia?>

    suspend fun getByPatient(patientId: Long): Result<List<ClinicalMedia>>

    suspend fun getByEncounter(encounterId: Long): Result<List<ClinicalMedia>>

    suspend fun getByPatientAndCategory(patientId: Long, category: String): Result<List<ClinicalMedia>>

    suspend fun getByStatus(status: ClinicalMediaStatus): Result<List<ClinicalMedia>>

    suspend fun updateStatus(id: Long, status: ClinicalMediaStatus): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>

    suspend fun countByPatient(patientId: Long): Result<Int>
}
