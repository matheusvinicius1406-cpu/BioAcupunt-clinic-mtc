package com.bioacupunt.patient.domain.repository

import com.bioacupunt.patient.domain.model.Patient

interface PatientRepository {
    suspend fun list(): List<Patient>
    suspend fun create(patient: Patient): Patient
    suspend fun getById(id: Long): Patient?
}
