package com.bioacupunt.prontuario.data.repository

import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.data.local.AllergyEntity
import com.bioacupunt.prontuario.data.local.ExameDao
import com.bioacupunt.prontuario.data.local.LabExamEntity
import com.bioacupunt.prontuario.data.local.MedicationEntity
import com.bioacupunt.prontuario.data.local.VitalSignEntity
import com.bioacupunt.prontuario.data.local.toDomain
import com.bioacupunt.prontuario.domain.model.Allergy
import com.bioacupunt.prontuario.domain.model.LabExam
import com.bioacupunt.prontuario.domain.model.Medication
import com.bioacupunt.prontuario.domain.model.VitalSign
import com.bioacupunt.prontuario.domain.repository.ExameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant

class ExameRepositoryImpl(private val dao: ExameDao) : ExameRepository {

    override fun observeVitals(patientId: Long): Flow<List<VitalSign>> =
        dao.observeVitals(patientId).map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun saveVital(vital: VitalSign): Result<VitalSign> = runCatching {
        val now = Instant.now().toString()
        val id = dao.saveVital(
            VitalSignEntity(
                id = vital.id, patientId = vital.patientId, label = vital.label, value = vital.value,
                recordedAt = vital.recordedAt.ifBlank { now }, createdAt = vital.createdAt.ifBlank { now }, updatedAt = now,
            )
        )
        vital.copy(id = if (vital.id != 0L) vital.id else id)
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override suspend fun deleteVital(id: Long): Result<Boolean> = runCatching {
        dao.deleteVital(id); true
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override fun observeExams(patientId: Long): Flow<List<LabExam>> =
        dao.observeExams(patientId).map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun saveExam(exam: LabExam): Result<LabExam> = runCatching {
        val now = Instant.now().toString()
        val id = dao.saveExam(
            LabExamEntity(
                id = exam.id, patientId = exam.patientId, name = exam.name, date = exam.date.ifBlank { now },
                resultTag = exam.resultTag.name, notes = exam.notes, createdAt = exam.createdAt.ifBlank { now }, updatedAt = now,
            )
        )
        exam.copy(id = if (exam.id != 0L) exam.id else id)
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override suspend fun deleteExam(id: Long): Result<Boolean> = runCatching {
        dao.deleteExam(id); true
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override fun observeMedications(patientId: Long): Flow<List<Medication>> =
        dao.observeMedications(patientId).map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun saveMedication(medication: Medication): Result<Medication> = runCatching {
        val now = Instant.now().toString()
        val id = dao.saveMedication(
            MedicationEntity(
                id = medication.id, patientId = medication.patientId, name = medication.name, info = medication.info,
                active = medication.active, createdAt = medication.createdAt.ifBlank { now }, updatedAt = now,
            )
        )
        medication.copy(id = if (medication.id != 0L) medication.id else id)
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override suspend fun deleteMedication(id: Long): Result<Boolean> = runCatching {
        dao.deleteMedication(id); true
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override fun observeAllergies(patientId: Long): Flow<List<Allergy>> =
        dao.observeAllergies(patientId).map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun saveAllergy(allergy: Allergy): Result<Allergy> = runCatching {
        val now = Instant.now().toString()
        val id = dao.saveAllergy(
            AllergyEntity(
                id = allergy.id, patientId = allergy.patientId, description = allergy.description,
                createdAt = allergy.createdAt.ifBlank { now }, updatedAt = now,
            )
        )
        allergy.copy(id = if (allergy.id != 0L) allergy.id else id)
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })

    override suspend fun deleteAllergy(id: Long): Result<Boolean> = runCatching {
        dao.deleteAllergy(id); true
    }.fold({ Result.Success(it) }, { Result.Error(AppError.DatabaseError()) })
}
