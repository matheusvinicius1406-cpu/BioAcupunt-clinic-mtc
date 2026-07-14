package com.bioacupunt.prontuario.data.repository

import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.data.local.ProntuarioDocumentDao
import com.bioacupunt.prontuario.data.local.ProntuarioDocumentEntity
import com.bioacupunt.prontuario.domain.model.ProntuarioDocument
import com.bioacupunt.prontuario.domain.repository.ProntuarioDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class ProntuarioDocumentRepositoryImpl(private val dao: ProntuarioDocumentDao) : ProntuarioDocumentRepository {

    override fun observe(patientId: Long): Flow<List<ProntuarioDocument>> =
        dao.observe(patientId).map { list ->
            list.map {
                ProntuarioDocument(
                    id = it.id, patientId = it.patientId, name = it.name, uri = it.uri,
                    mimeType = it.mimeType, sizeBytes = it.sizeBytes, addedAt = it.addedAt,
                    createdAt = it.createdAt, updatedAt = it.updatedAt,
                )
            }
        }.catch { emit(emptyList()) }

    override suspend fun save(document: ProntuarioDocument): Result<ProntuarioDocument> = runCatching {
        val now = Instant.now().toString()
        val id = dao.save(
            ProntuarioDocumentEntity(
                id = document.id, patientId = document.patientId, name = document.name, uri = document.uri,
                mimeType = document.mimeType, sizeBytes = document.sizeBytes,
                addedAt = document.addedAt.ifBlank { now }, createdAt = document.createdAt.ifBlank { now }, updatedAt = now,
            )
        )
        document.copy(id = if (document.id != 0L) document.id else id)
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override suspend fun delete(id: Long): Result<Boolean> = runCatching {
        dao.delete(id); true
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })
}
