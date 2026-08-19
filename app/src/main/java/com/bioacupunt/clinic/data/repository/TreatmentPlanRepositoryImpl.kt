package com.bioacupunt.clinic.data.repository

import com.bioacupunt.clinic.data.local.TreatmentPlanDao
import com.bioacupunt.clinic.data.local.TreatmentPlanEntity
import com.bioacupunt.clinic.domain.model.TreatmentPlan
import com.bioacupunt.clinic.domain.model.TreatmentPlanStatus
import com.bioacupunt.clinic.domain.repository.TreatmentPlanRepository

class TreatmentPlanRepositoryImpl(
    private val dao: TreatmentPlanDao,
    private val tenantId: () -> Long,
) : TreatmentPlanRepository {

    override suspend fun getById(id: Long): TreatmentPlan? =
        dao.getById(id)?.toDomain()

    override suspend fun getByEncounterId(encounterId: Long): TreatmentPlan? =
        dao.getByEncounterId(encounterId)?.toDomain()

    override suspend fun getByPatientId(patientId: Long): List<TreatmentPlan> =
        dao.getByPatientId(patientId).map { it.toDomain() }

    override suspend fun create(plan: TreatmentPlan): Long =
        dao.insert(plan.toEntity(tenantId()))

    override suspend fun update(plan: TreatmentPlan) =
        dao.update(plan.toEntity(tenantId()))

    override suspend fun delete(id: Long) {
        val now = java.time.Instant.now().toString()
        dao.softDelete(id, now)
    }

    private fun TreatmentPlanEntity.toDomain() = TreatmentPlan(
        id = id,
        tenantId = tenantId,
        encounterId = encounterId,
        patientId = patientId,
        goals = goals,
        principles = principles,
        items = emptyList(), // parsed from itemsJson when needed
        frequency = frequency,
        duration = duration,
        followUpRecommendation = followUpRecommendation,
        status = TreatmentPlanStatus.entries.firstOrNull { it.name == status } ?: TreatmentPlanStatus.DRAFT,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun TreatmentPlan.toEntity(tid: Long) = TreatmentPlanEntity(
        id = id,
        tenantId = tid,
        encounterId = encounterId,
        patientId = patientId,
        goals = goals,
        principles = principles,
        itemsJson = "[]",
        frequency = frequency,
        duration = duration,
        followUpRecommendation = followUpRecommendation,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
