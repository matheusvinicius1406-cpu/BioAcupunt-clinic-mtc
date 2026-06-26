package com.bioacupunt.patient.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioacupunt.di.PatientDataModule
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.sync.SyncScheduler
import com.bioacupunt.sync.data.local.SyncQueueDao

@Composable
fun rememberPatientsViewModel(): PatientsViewModel {
    val appDatabase = com.bioacupunt.di.DatabaseModule.provideAppDatabase(localContext.current)
    val api = PatientDataModule.providePatientApi()
    val dao = PatientDataModule.providePatientDao(appDatabase)
    val syncQueueDao = PatientDataModule.provideSyncQueueDao(appDatabase)
    val scheduler = PatientDataModule.provideSyncScheduler(localContext.current)
    val repository = PatientDataModule.providePatientRepository(api, appDatabase, scheduler)
    val viewModel: PatientsViewModel = viewModel(
        factory = PatientsViewModelFactory(
            getPatients = GetPatients(repository),
            createPatient = CreatePatient(repository),
            syncScheduler = scheduler
        )
    )
    return viewModel
}

class PatientsViewModelFactory(
    private val getPatients: GetPatients,
    private val createPatient: CreatePatient,
    private val syncScheduler: SyncScheduler
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PatientsViewModel(getPatients, createPatient, syncScheduler) as T
    }
}

class PatientsViewModel(
    private val getPatients: GetPatients,
    private val createPatient: CreatePatient,
    private val syncScheduler: SyncScheduler
) : androidx.lifecycle.ViewModel() {

    private val _state = androidx.compose.runtime.mutableStateOf(PatientsUiState())
    val state: androidx.compose.runtime.State<PatientsUiState> = _state

    fun onEvent(event: PatientsEvent) {
        when (event) {
            PatientsEvent.OnLoad -> Unit
            is PatientsEvent.CreatePatient -> Unit
            PatientsEvent.OnCreateClick -> Unit
            is PatientsEvent.OnErrorShown -> _state.value = _state.value.copy(error = null)
        }
    }
}
