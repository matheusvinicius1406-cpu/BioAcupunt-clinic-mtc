package com.bioacupunt.pharma.data.repository

import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.pharma.data.local.PrescricaoDao
import com.bioacupunt.pharma.data.local.toDomain
import com.bioacupunt.pharma.data.local.toEntity
import com.bioacupunt.pharma.domain.model.Prescricao
import com.bioacupunt.pharma.domain.repository.PrescricaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class PrescricaoRepositoryImpl(
    private val dao: PrescricaoDao,
    private val tenantManager: TenantManager,
) : PrescricaoRepository {

    private val tenantId: Long get() = tenantManager.requireTenantId()

    override fun observeActiveByPatient(patientId: Long): Flow<List<Prescricao>> =
        dao.observeActiveByPatient(patientId, tenantId)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(prescricao: Prescricao): Result<Prescricao> = try {
        val now = Instant.now().toString()
        val owned = prescricao.copy(
            tenantId = tenantId,
            prescritoEm = prescricao.prescritoEm.ifBlank { now },
        )
        val savedId = dao.save(owned.toEntity())
        Result.Success(owned.copy(id = if (owned.id == 0L) savedId else owned.id))
    } catch (e: Exception) {
        Result.Error(AppError.from(e))
    }

    override suspend fun deactivate(id: Long): Result<Boolean> = try {
        dao.deactivate(id, tenantId)
        Result.Success(true)
    } catch (e: Exception) {
        Result.Error(AppError.from(e))
    }
}
