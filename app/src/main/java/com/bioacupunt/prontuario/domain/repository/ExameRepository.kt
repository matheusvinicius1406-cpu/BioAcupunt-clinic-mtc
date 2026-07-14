package com.bioacupunt.prontuario.domain.repository

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Allergy
import com.bioacupunt.prontuario.domain.model.LabExam
import com.bioacupunt.prontuario.domain.model.Medication
import com.bioacupunt.prontuario.domain.model.VitalSign
import kotlinx.coroutines.flow.Flow

interface ExameRepository {
    fun observeVitals(patientId: Long): Flow<List<VitalSign>>
    suspend fun saveVital(vital: VitalSign): Result<VitalSign>
    suspend fun deleteVital(id: Long): Result<Boolean>

    fun observeExams(patientId: Long): Flow<List<LabExam>>
    suspend fun saveExam(exam: LabExam): Result<LabExam>
    suspend fun deleteExam(id: Long): Result<Boolean>

    fun observeMedications(patientId: Long): Flow<List<Medication>>
    suspend fun saveMedication(medication: Medication): Result<Medication>
    suspend fun deleteMedication(id: Long): Result<Boolean>

    fun observeAllergies(patientId: Long): Flow<List<Allergy>>
    suspend fun saveAllergy(allergy: Allergy): Result<Allergy>
    suspend fun deleteAllergy(id: Long): Result<Boolean>
}
