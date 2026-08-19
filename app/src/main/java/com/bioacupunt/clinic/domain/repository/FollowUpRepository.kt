package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.clinic.domain.model.FollowUpStatus

interface FollowUpRepository {
    suspend fun getById(id: Long): FollowUp?
    suspend fun getByPatientId(patientId: Long): List<FollowUp>
    suspend fun getByPatientIdAndStatus(patientId: Long, status: FollowUpStatus): List<FollowUp>
    suspend fun getUpcoming(): List<FollowUp>
    suspend fun create(followUp: FollowUp): Long
    suspend fun update(followUp: FollowUp)
    suspend fun complete(id: Long, actualFindings: String)
    suspend fun delete(id: Long)
}
