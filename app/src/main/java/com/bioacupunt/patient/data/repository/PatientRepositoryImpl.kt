package com.bioacupunt.patient.data.repository

import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.patient.data.local.PatientEntity
import com.bioacupunt.patient.domain.model.Patient
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.sync.SyncQueueEntity
import com.bioacupunt.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PatientRepositoryImpl(
    private val api: PatientApi,
    private val db: AppDatabase,
    private val scheduler: SyncScheduler
) : PatientRepository {

    override fun list(): Flow<List<Patient>> =
        db.patientDao().getAllPatients().map { entities -> entities.map { it.toDomain() } }

    override suspend fun create(patient: Patient): Patient {
        val entity = PatientEntity(
            id = patient.id,
            tenantId = patient.tenantId,
            name = patient.name,
            document = patient.document,
            createdAt = patient.createdAt,
            updatedAt = patient.updatedAt,
            status = patient.status,
            pendingSync = true
        )
        db.patientDao().save(entity)

        val payload = Json.encodeToString(mapOf("patient" to patient))

        db.patientDao().getById(patient.id)?.let { saved ->
            db.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = "Patient",
                    entityId = saved.id.toString(),
                    operation = "CREATE",
                    payloadJson = payload,
                    status = "PENDING"
                )
            )
            scheduler.scheduleSync()
        }

        return entity.toDomain()
    }

    override suspend fun getById(id: Long): Patient? =
        db.patientDao().getById(id)?.toDomain()
}
