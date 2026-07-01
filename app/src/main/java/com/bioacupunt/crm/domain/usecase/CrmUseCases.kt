package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.Flow

class GetCrmPatients(
    private val repository: CrmPatientRepository
) {
    operator fun invoke(stage: String? = null): Flow<List<CrmPatient>> {
        return if (stage.isNullOrBlank()) repository.observeAll() else repository.observeByStage(stage)
    }
}

class SearchCrmPatients(
    private val repository: CrmPatientRepository
) {
    operator fun invoke(query: String): Flow<List<CrmPatient>> {
        return repository.search(query)
    }
}

class SaveCrmPatient(
    private val repository: CrmPatientRepository
) {
    suspend operator fun invoke(patient: CrmPatient): Result<CrmPatient> {
        return repository.save(patient)
    }
}

class UpdateCrmStage(
    private val repository: CrmPatientRepository
) {
    suspend operator fun invoke(patientId: Long, stage: PatientStage): Result<CrmPatient> {
        val current = repository.getById(patientId)
        if (current is Result.Error) return current
        val patient = (current as Result.Success).data.copy(stage = stage.name)
        return repository.save(patient)
    }
}
