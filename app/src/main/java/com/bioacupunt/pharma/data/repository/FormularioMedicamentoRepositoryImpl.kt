package com.bioacupunt.pharma.data.repository

import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.data.local.FormularioMedicamentoDao
import com.bioacupunt.pharma.data.local.toDomain
import com.bioacupunt.pharma.data.local.toEntity
import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import com.bioacupunt.pharma.domain.repository.FormularioMedicamentoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class FormularioMedicamentoRepositoryImpl(
    private val dao: FormularioMedicamentoDao,
) : FormularioMedicamentoRepository {

    override suspend fun getById(medicamentoId: String, tenantId: Long): FormularioMedicamento? =
        dao.getById(medicamentoId, tenantId)?.toDomain()

    override suspend fun save(formulario: FormularioMedicamento): Result<FormularioMedicamento> = try {
        val toSave = formulario.copy(atualizadoEm = Instant.now().toString())
        dao.save(toSave.toEntity())
        Result.Success(toSave)
    } catch (e: Exception) {
        Result.Error(AppError.from(e))
    }

    override suspend fun approve(medicamentoId: String, tenantId: Long, autor: String): Result<FormularioMedicamento> = try {
        val existing = dao.getById(medicamentoId, tenantId)?.toDomain()
        if (existing == null) {
            Result.Error(AppError.ValidationError("Nenhum rascunho encontrado pra aprovar — salve antes de aprovar."))
        } else if (!existing.meetsApprovalMinimum) {
            Result.Error(AppError.ValidationError("Preencha ao menos posologia adulto e via de administração antes de aprovar."))
        } else {
            val approved = existing.copy(
                status = FormularioStatus.APROVADO,
                autor = autor,
                atualizadoEm = Instant.now().toString(),
            )
            dao.save(approved.toEntity())
            Result.Success(approved)
        }
    } catch (e: Exception) {
        Result.Error(AppError.from(e))
    }

    override fun observeApproved(tenantId: Long): Flow<List<FormularioMedicamento>> =
        dao.observeApproved(tenantId)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun getApprovedByIds(ids: List<String>, tenantId: Long): List<FormularioMedicamento> =
        if (ids.isEmpty()) emptyList()
        else runCatching { dao.getApprovedByIds(ids, tenantId).map { it.toDomain() } }.getOrDefault(emptyList())
}
