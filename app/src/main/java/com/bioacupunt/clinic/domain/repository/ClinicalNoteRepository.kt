package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.ClinicalNote

interface ClinicalNoteRepository {
    suspend fun getById(id: Long): ClinicalNote?
    suspend fun getByEncounterId(encounterId: Long): ClinicalNote?
    suspend fun getByPatientId(patientId: Long): List<ClinicalNote>
    suspend fun create(note: ClinicalNote): Long
    suspend fun update(note: ClinicalNote)
    suspend fun finalizeNote(id: Long, finalizedBy: String)
    suspend fun delete(id: Long)
}
