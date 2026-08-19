package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.Encounter

interface EncounterRepository {
    suspend fun getById(id: Long): Encounter?
    suspend fun getByPatientId(patientId: Long): List<Encounter>
    suspend fun getRecent(patientId: Long, limit: Int = 10): List<Encounter>
    suspend fun getActive(patientId: Long): Encounter?
    suspend fun create(encounter: Encounter): Long
    suspend fun update(encounter: Encounter)
    suspend fun complete(id: Long)
    suspend fun cancel(id: Long)
    suspend fun countByPatientId(patientId: Long): Int
}
