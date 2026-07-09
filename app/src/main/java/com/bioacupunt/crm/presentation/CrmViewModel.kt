package com.bioacupunt.crm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.model.PatientStage
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.crm.domain.usecase.SearchCrmPatients
import com.bioacupunt.crm.domain.usecase.SaveCrmPatient
import com.bioacupunt.crm.domain.usecase.UpdateCrmStage
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class CrmUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val selectedStageName: String? = null,
    val items: List<CrmPatient> = emptyList(),
    val filteredPatients: List<CrmPatient> = emptyList(),
    val reportSummary: Map<String, Any> = emptyMap(),
    val stages: List<PatientStage> = PatientStage.entries,
    val error: String? = null
)

class CrmViewModelFactory(
    private val saveCrmPatient: SaveCrmPatient,
    private val updateCrmStage: UpdateCrmStage,
    private val searchCrmPatients: SearchCrmPatients,
    private val repository: CrmPatientRepository? = null,
    private val tenantManager: TenantManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CrmViewModel(saveCrmPatient, updateCrmStage, searchCrmPatients, repository, tenantManager) as T
    }
}

class CrmViewModel(
    private val saveCrmPatient: SaveCrmPatient,
    private val updateCrmStage: UpdateCrmStage,
    private val searchCrmPatients: SearchCrmPatients,
    private val repository: CrmPatientRepository,
    private val tenantManager: TenantManager
) : ViewModel() {

    val tenantId: Long get() = tenantManager.currentTenantId() ?: 0L

    private val _state = MutableStateFlow(CrmUiState())
    val state: StateFlow<CrmUiState> = _state.asStateFlow()

    init {
        observePatients()
        computeSummary()
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        filterPatients()
    }

    fun onStageSelected(stageName: String?) {
        _state.update { it.copy(selectedStageName = stageName) }
        observePatients()
    }

    fun createPatient(
        name: String,
        phone: String = "",
        email: String = "",
        birthDate: String = "",
        notes: String = ""
    ) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Nome é obrigatório.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val now = java.time.Instant.now().toString()
            val patient = CrmPatient(
                id = 0,
                tenantId = tenantId ?: 0L,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                birthDate = birthDate,
                stage = PatientStage.FIRST_CONTACT.name,
                totalSessions = 0,
                totalRevenueBrl = 0.0,
                lastVisit = "",
                nextAppointment = "",
                tags = emptyList(),
                notes = notes,
                referralSource = "",
                npsScore = null,
                healthInsurance = "",
                mainComplaint = "",
                createdAt = now
            )
            val result = saveCrmPatient(patient)
            if (result is Result.Error) {
                _state.update { it.copy(isLoading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateStage(patientId: Long, stage: PatientStage) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = updateCrmStage(patientId, stage)
            if (result is Result.Error) {
                _state.update { it.copy(isLoading = false, error = result.kind.userMessage) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deletePatient(patientId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repository?.let { repo ->
                runCatching { repo.getById(patientId) }
                    .mapCatching { r -> r as? Result.Success }
                    .getOrNull()
            }
            when (val current = result?.data) {
                is Result.Success -> {
                    val updated = current.data.copy(notes = "[SOFT_DELETED]")
                    val saveResult = saveCrmPatient(updated)
                    if (saveResult is Result.Error) {
                        _state.update { it.copy(isLoading = false, error = saveResult.kind.userMessage) }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
                else -> _state.update { it.copy(isLoading = false, error = "Paciente não encontrado.") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun observePatients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val baseFlow = if (_state.value.selectedStageName.isNullOrBlank()) {
                searchCrmPatients("")
            } else {
                searchCrmPatients(_state.value.selectedStageName!!)
            }
            baseFlow
                .catch { e -> _state.update { it.copy(isLoading = false, error = it.error ?: e.localizedMessage.orEmpty()) } }
                .collect { list ->
                    _state.update { current ->
                        current.copy(
                            items = list,
                            filteredPatients = if (current.query.isBlank()) list else filterQuery(list, current.query),
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun filterPatients() {
        val current = _state.value
        _state.update { current.copy(filteredPatients = filterQuery(current.items, current.query)) }
    }

    private fun computeSummary() {
        viewModelScope.launch {
            try {
                val repo = repository ?: return@launch
                val total = repo.count()
                val active = repo.countByStage(PatientStage.ACTIVE.name)
                val revenue = repo.sumRevenue(java.time.LocalDate.now().minusDays(30).toString(), java.time.LocalDate.now().toString())
                val summary = mapOf(
                    "total" to total,
                    "active" to active,
                    "revenue" to (revenue as? Result.Success)?.data ?: 0.0,
                    "retention" to if (total > 0) active.toDouble() / total else 0.0
                )
                _state.update { it.copy(reportSummary = summary) }
            } catch (e: Exception) {
                // best-effort summary only
            }
        }
    }

    private fun filterQuery(items: List<CrmPatient>, query: String): List<CrmPatient> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items
        return items.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
    }
}
