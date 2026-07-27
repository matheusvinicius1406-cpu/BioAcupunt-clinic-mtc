package com.bioacupunt.pharma.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.domain.model.Prescricao
import kotlinx.coroutines.flow.Flow

interface PrescricaoRepository {
    fun observeActiveByPatient(patientId: Long): Flow<List<Prescricao>>
    suspend fun save(prescricao: Prescricao): Result<Prescricao>
    suspend fun deactivate(id: Long): Result<Boolean>
}
