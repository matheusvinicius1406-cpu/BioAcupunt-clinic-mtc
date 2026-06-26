package com.bioacupunt.patient.domain.usecase

import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow

class GetPatients(private val repository: PatientRepository) {
    operator fun invoke(): Flow<List<Patient>> = repository.list()
}
