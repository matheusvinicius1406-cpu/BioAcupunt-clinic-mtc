package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.TreatmentPlan

interface TreatmentPlanRepository {
    suspend fun getById(id: Long): TreatmentPlan?
    suspend fun getByEncounterId(encounterId: Long): TreatmentPlan?
    suspend fun getByPatientId(patientId: Long): List<TreatmentPlan>
    suspend fun create(plan: TreatmentPlan): Long
    suspend fun update(plan: TreatmentPlan)
    suspend fun delete(id: Long)
}
