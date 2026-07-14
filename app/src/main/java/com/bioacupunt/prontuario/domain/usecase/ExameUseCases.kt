package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Allergy
import com.bioacupunt.prontuario.domain.model.LabExam
import com.bioacupunt.prontuario.domain.model.Medication
import com.bioacupunt.prontuario.domain.model.VitalSign
import com.bioacupunt.prontuario.domain.repository.ExameRepository
import kotlinx.coroutines.flow.Flow

class ObserveVitals(private val repository: ExameRepository) {
    operator fun invoke(patientId: Long): Flow<List<VitalSign>> = repository.observeVitals(patientId)
}
class SaveVital(private val repository: ExameRepository) {
    suspend operator fun invoke(vital: VitalSign): Result<VitalSign> = repository.saveVital(vital)
}
class DeleteVital(private val repository: ExameRepository) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.deleteVital(id)
}

class ObserveExams(private val repository: ExameRepository) {
    operator fun invoke(patientId: Long): Flow<List<LabExam>> = repository.observeExams(patientId)
}
class SaveExam(private val repository: ExameRepository) {
    suspend operator fun invoke(exam: LabExam): Result<LabExam> = repository.saveExam(exam)
}
class DeleteExam(private val repository: ExameRepository) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.deleteExam(id)
}

class ObserveMedications(private val repository: ExameRepository) {
    operator fun invoke(patientId: Long): Flow<List<Medication>> = repository.observeMedications(patientId)
}
class SaveMedication(private val repository: ExameRepository) {
    suspend operator fun invoke(medication: Medication): Result<Medication> = repository.saveMedication(medication)
}
class DeleteMedication(private val repository: ExameRepository) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.deleteMedication(id)
}

class ObserveAllergies(private val repository: ExameRepository) {
    operator fun invoke(patientId: Long): Flow<List<Allergy>> = repository.observeAllergies(patientId)
}
class SaveAllergy(private val repository: ExameRepository) {
    suspend operator fun invoke(allergy: Allergy): Result<Allergy> = repository.saveAllergy(allergy)
}
class DeleteAllergy(private val repository: ExameRepository) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.deleteAllergy(id)
}

class ExameUseCases(
    val observeVitals: ObserveVitals,
    val saveVital: SaveVital,
    val deleteVital: DeleteVital,
    val observeExams: ObserveExams,
    val saveExam: SaveExam,
    val deleteExam: DeleteExam,
    val observeMedications: ObserveMedications,
    val saveMedication: SaveMedication,
    val deleteMedication: DeleteMedication,
    val observeAllergies: ObserveAllergies,
    val saveAllergy: SaveAllergy,
    val deleteAllergy: DeleteAllergy,
) {
    constructor(repository: ExameRepository) : this(
        observeVitals = ObserveVitals(repository),
        saveVital = SaveVital(repository),
        deleteVital = DeleteVital(repository),
        observeExams = ObserveExams(repository),
        saveExam = SaveExam(repository),
        deleteExam = DeleteExam(repository),
        observeMedications = ObserveMedications(repository),
        saveMedication = SaveMedication(repository),
        deleteMedication = DeleteMedication(repository),
        observeAllergies = ObserveAllergies(repository),
        saveAllergy = SaveAllergy(repository),
        deleteAllergy = DeleteAllergy(repository),
    )
}
