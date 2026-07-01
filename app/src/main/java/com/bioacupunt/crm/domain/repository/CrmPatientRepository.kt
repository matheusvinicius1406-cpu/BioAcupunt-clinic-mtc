package com.bioacupunt.crm.domain.repository

import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

interface CrmPatientRepository {
    fun observeAll(): Flow<List<CrmPatient>>
    fun observeByStage(stage: String): Flow<List<CrmPatient>>
    fun search(query: String): Flow<List<CrmPatient>>
    suspend fun getById(id: Long): Result<CrmPatient>
    suspend fun save(entity: CrmPatient): Result<CrmPatient>
    suspend fun saveAll(entities: List<CrmPatient>): Result<Int>
    suspend fun stageCount(stage: String): Result<Int>
    suspend fun getPendingSync(since: String): Result<List<CrmPatient>>
}
