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

    override fun observeNextUpcoming(fromDate: String, fromTime: String): Flow<Appointment?> {
        return dao.observeNextUpcoming(fromDate, fromTime, tenantId)
            .map { it?.toDomain() }
            .catch { emit(null) }
    }

    override suspend fun getById(id: Long): Result<Appointment> {
        return try {
            val entity = dao.getById(id, tenantId)
            if (entity == null) Result.Error(AppError.DatabaseError())
            else Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
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
            val owned = appointment.copy(tenantId = resolveTenant(appointment.tenantId))
            val now = java.time.Instant.now().toString()
            val savedId = dao.save(owned.toEntity(now, identity = identityFor(owned.id)))
            val saved = owned.copy(id = if (owned.id == 0L) savedId else owned.id)
            Result.Success(saved)
        } catch (e: Exception) {
            com.bioacupunt.observability.AppLogger.e("AppointmentRepository", "save failed", e)
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun countByDate(date: String): Result<Int> {
        return try {
            Result.Success(dao.countByDate(date, tenantId))
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    override suspend fun countByStatus(status: String): Result<Int> {
        return try {
            Result.Success(dao.countByStatus(status, tenantId))
        } catch (e: Exception) {
            Result.Error(AppError.from(e))
        }
    }

    /**
     * Returns the tenant this row belongs to, stamping the current one when the
     * caller did not set it.
     *
     * The previous version rejected any mismatch, including the unset default of
     * 0 — which is what every screen sends, because a UI has no business knowing
     * tenant ids. The result was that *no appointment could ever be saved*: the
     * mismatch threw, the catch below turned it into "Falha ao acessar dados
     * locais", and the failure looked like broken storage rather than a
     * misplaced check.
     *
     * A genuine cross-tenant write — a non-zero id belonging to someone else —
     * is still refused. That is the case this guard was actually for.
     */
    /** See CrmPatientRepositoryImpl.identityFor — same reasoning. */
    private suspend fun identityFor(id: Long): com.bioacupunt.sync.SyncIdentity {
        if (id == 0L) return com.bioacupunt.sync.SyncIdentity.new()
        val existing = dao.getById(id, tenantId) ?: return com.bioacupunt.sync.SyncIdentity.new()
        return com.bioacupunt.sync.SyncIdentity.carryForward(
            clientId = existing.clientId,
            serverId = existing.serverId,
            baseRev = existing.baseRev,
        )
    }

    private fun resolveTenant(entityTenantId: Long): Long {
        val current = tenantManager.requireTenantId()
        if (entityTenantId != 0L && entityTenantId != current) {
            throw IllegalArgumentException(
                "Tenant mismatch on appointment operation: $entityTenantId != $current"
            )
        }
        return current
    }
}
