package com.bioacupunt.agenda.data.repository

import com.bioacupunt.agenda.data.local.AppointmentDao
import com.bioacupunt.agenda.data.local.AppointmentEntity
import com.bioacupunt.agenda.data.local.toDomain
import com.bioacupunt.agenda.data.local.toEntity
import com.bioacupunt.agenda.domain.model.Appointment
import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppointmentRepositoryImpl(
    private val dao: AppointmentDao,
    private val tenantManager: TenantManager
) : AppointmentRepository {

    private val tenantId: Long get() = tenantManager.requireTenantId()

    override fun observeByDate(date: String): Flow<List<Appointment>> {
        return dao.observeByDate(date, tenantId)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeByPatient(patientId: Long): Flow<List<Appointment>> {
        return dao.observeByPatient(patientId, tenantId)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeByStatus(status: String): Flow<List<Appointment>> {
        return dao.observeByStatus(status, tenantId)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeBetween(start: String, end: String): Flow<List<Appointment>> {
        return dao.observeBetween(start, end, tenantId)
            .map { it.map { e -> e.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun getById(id: Long): Result<Appointment> {
        return try {
            val entity = dao.getById(id, tenantId)
            if (entity == null) Result.Error(AppError.DatabaseError())
            else Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun getByDateSync(date: String): List<Appointment> {
        return try {
            dao.getByDateSync(date, tenantId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun save(appointment: Appointment): Result<Appointment> {
        return try {
            validateTenant(appointment.tenantId)
            val now = java.time.Instant.now().toString()
            val savedId = dao.save(appointment.toEntity(now))
            val saved = appointment.copy(id = if (appointment.id == 0L) savedId else appointment.id)
            Result.Success(saved)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun countByDate(date: String): Result<Int> {
        return try {
            Result.Success(dao.countByDate(date, tenantId))
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    override suspend fun countByStatus(status: String): Result<Int> {
        return try {
            Result.Success(dao.countByStatus(status, tenantId))
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e))
        }
    }

    private fun validateTenant(entityTenantId: Long) {
        val current = tenantManager.currentTenantId()
        if (current != null && entityTenantId != current) {
            throw IllegalArgumentException("Tenant mismatch on appointment operation")
        }
    }
}
