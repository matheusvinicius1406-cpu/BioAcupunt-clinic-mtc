package com.bioacupunt.prontuario.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.ProntuarioDocument
import kotlinx.coroutines.flow.Flow

interface ProntuarioDocumentRepository {
    fun observe(patientId: Long): Flow<List<ProntuarioDocument>>
    suspend fun save(document: ProntuarioDocument): Result<ProntuarioDocument>
    suspend fun delete(id: Long): Result<Boolean>
}
