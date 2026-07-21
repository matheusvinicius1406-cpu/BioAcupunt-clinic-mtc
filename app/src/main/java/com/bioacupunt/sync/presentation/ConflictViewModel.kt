package com.bioacupunt.sync.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioacupunt.observability.AppLogger
import com.bioacupunt.sync.SyncEngine
import com.bioacupunt.sync.data.local.SyncConflictDao
import com.bioacupunt.sync.data.local.SyncConflictEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** One field where the two versions disagree. */
data class ConflictField(
    val label: String,
    val localValue: String,
    val serverValue: String,
) {
    val differs: Boolean get() = localValue != serverValue
}

data class ConflictItem(
    val id: Long,
    val entityLabel: String,
    val title: String,
    val detectedAt: String,
    val fields: List<ConflictField>,
)

data class ConflictUiState(
    val items: List<ConflictItem> = emptyList(),
    val isResolving: Boolean = false,
    val error: String? = null,
)

class ConflictViewModelFactory(
    private val conflictDao: SyncConflictDao,
    private val engine: SyncEngine,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ConflictViewModel(conflictDao, engine) as T
    }
}

class ConflictViewModel(
    private val conflictDao: SyncConflictDao,
    private val engine: SyncEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(ConflictUiState())
    val state: StateFlow<ConflictUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            conflictDao.observeUnresolved()
                .catch { e -> _state.update { it.copy(error = e.localizedMessage) } }
                .collect { rows ->
                    _state.update { it.copy(items = rows.map { row -> row.toItem() }) }
                }
        }
    }

    fun keepLocal(conflictId: Long) = resolve(conflictId) { engine.resolveKeepingLocal(it) }

    fun keepServer(conflictId: Long) = resolve(conflictId) { engine.resolveKeepingServer(it) }

    private fun resolve(conflictId: Long, block: suspend (Long) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isResolving = true, error = null) }
            runCatching { block(conflictId) }
                .onFailure { e ->
                    AppLogger.e("ConflictViewModel", "Failed to resolve conflict $conflictId", e)
                    _state.update {
                        it.copy(
                            isResolving = false,
                            // The conflict deliberately stays on screen. A
                            // failed resolution that vanished would look like a
                            // decision that took effect.
                            error = "Não foi possível aplicar a escolha. Tente novamente.",
                        )
                    }
                }
                .onSuccess { _state.update { it.copy(isResolving = false) } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

private val FIELD_LABELS = mapOf(
    "name" to "Nome",
    "phone" to "Telefone",
    "email" to "E-mail",
    "stage" to "Etapa",
    "notes" to "Observações",
    "status" to "Status",
    "scheduled_at" to "Data e hora",
    "value_brl" to "Valor",
    "paid" to "Pago",
    "amount_brl" to "Valor",
    "occurred_on" to "Data",
    "method" to "Forma de pagamento",
    "category" to "Categoria",
)

private val ENTITY_LABELS = mapOf(
    "patient" to "Paciente",
    "appointment" to "Consulta",
    "transaction" to "Lançamento",
)

internal fun SyncConflictEntity.toItem(): ConflictItem {
    val local = localPayloadJson.parseJson()
    val server = serverPayloadJson.parseJson()

    // Union of both sides: a field present on only one of them is exactly the
    // kind of difference she needs to see, so it must not be dropped by
    // iterating over one side alone.
    val keys = (local.keys + server.keys).sortedBy { FIELD_LABELS[it] ?: it }

    val fields = keys.map { key ->
        ConflictField(
            label = FIELD_LABELS[key] ?: key,
            localValue = local[key].orEmpty(),
            serverValue = server[key].orEmpty(),
        )
    }

    return ConflictItem(
        id = id,
        entityLabel = ENTITY_LABELS[entityType] ?: entityType,
        title = local["name"] ?: server["name"] ?: "Registro #${serverId ?: clientId}",
        detectedAt = detectedAt,
        fields = fields,
    )
}

private fun String.parseJson(): Map<String, String> = runCatching {
    val json = JSONObject(this)
    json.keys().asSequence().associateWith { key ->
        if (json.isNull(key)) "" else json.get(key).toString()
    }
}.getOrElse { emptyMap() }
