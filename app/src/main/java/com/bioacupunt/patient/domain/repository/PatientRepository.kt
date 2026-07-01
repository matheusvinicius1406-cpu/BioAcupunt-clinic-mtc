package com.bioacupunt.patient.domain.repository

import com.bioacupunt.patient.domain.model.Patient
import kotlinx.coroutines.flow.Flow

interface PatientRepository {
    fun list(): Flow<List<Patient>>
    suspend fun create(patient: Patient): Patient
    suspend fun getById(id: Long): Patient?
}
