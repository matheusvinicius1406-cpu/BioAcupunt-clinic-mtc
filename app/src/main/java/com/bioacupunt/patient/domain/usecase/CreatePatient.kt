package com.bioacupunt.patient.domain.usecase

import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.domain.repository.PatientRepository

class CreatePatient(private val repository: PatientRepository) {
    suspend operator fun invoke(patient: Patient): Patient = repository.create(patient)
}
