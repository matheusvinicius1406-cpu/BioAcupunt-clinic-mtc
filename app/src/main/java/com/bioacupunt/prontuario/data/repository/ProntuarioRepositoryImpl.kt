package com.bioacupunt.prontuario.data.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.data.local.ProntuarioDao
import com.bioacupunt.prontuario.data.local.ProntuarioEntryEntity
import com.bioacupunt.prontuario.data.local.ProntuarioEntity
import com.bioacupunt.prontuario.data.local.toDomain
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.repository.ProntuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class ProntuarioRepositoryImpl(private val dao: ProntuarioDao) : ProntuarioRepository {

    override fun observeProntuario(patientId: Long): Flow<Prontuario?> =
        dao.observe(patientId).map { it?.toDomain() }.catch { emit(null) }

    override suspend fun save(prontuario: Prontuario): Result<Prontuario> = runCatching {
        val now = Instant.now().toString()
        val entity = ProntuarioEntity(
            id = 0,
            patientId = prontuario.patientId,
            summary = prontuario.summary,
            mainComplaint = prontuario.mainComplaint,
            diagnosis = prontuario.diagnosis,
            treatmentPlan = prontuario.treatmentPlan,
            updatedAt = now
        )
        dao.save(entity)
        prontuario
    }.fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(com.bioacupunt.core.util.AppErrorKind.IO("Falha ao salvar prontuário")) })

    override fun observeEntries(patientId: Long): Flow<List<ProntuarioEntry>> =
        dao.observeEntries(patientId).map { list -> list.map { it.toDomain() } }

    override suspend fun addEntry(entry: ProntuarioEntry): Result<ProntuarioEntry> = runCatching {
        val now = Instant.now().toString()
        val entity = ProntuarioEntryEntity(
            id = 0,
            patientId = entry.patientId,
            doctorName = entry.doctorName,
            date = entry.date,
            type = entry.type.name,
            body = entry.body,
            attachmentsJson = entry.attachmentsJson,
            updatedAt = now
        )
        val id = dao.saveEntry(entity)
        entry.copy(id = if (id > 0) id else entry.id)
    }.fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(com.bioacupunt.core.util.AppErrorKind.IO("Falha ao adicionar entrada")) })

    override suspend fun updateEntry(entry: ProntuarioEntry): Result<ProntuarioEntry> = runCatching {
        val now = Instant.now().toString()
        val entity = ProntuarioEntryEntity(
            id = entry.id,
            patientId = entry.patientId,
            doctorName = entry.doctorName,
            date = entry.date,
            type = entry.type.name,
            body = entry.body,
            attachmentsJson = entry.attachmentsJson,
            updatedAt = now
        )
        dao.saveEntry(entity)
        entry
    }.fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(com.bioacupunt.core.util.AppErrorKind.IO("Falha ao atualizar entrada")) })

    override suspend fun deleteEntry(id: Long): Result<Boolean> = runCatching {
        dao.deleteEntry(id)
        true
    }.fold(onSuccess = { Result.Success(it) }, onFailure = { Result.Error(com.bioacupunt.core.util.AppErrorKind.IO("Falha ao remover entrada")) })

    override suspend fun getPendingSync(since: String): Result<List<ProntuarioEntry>> {
        // Placeholder: sync pendente via Room será integrado com fila downstream.
        return Result.Success(emptyList())
    }
}
