package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.Prontuario
import com.bioacupunt.prontuario.domain.model.ProntuarioEntry
import com.bioacupunt.prontuario.domain.repository.ProntuarioRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class GetProntuario(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    operator fun invoke(patientId: Long): Flow<Prontuario?> = repository.observeProntuario(patientId)
}

class SaveProntuario(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun invoke(prontuario: Prontuario): Result<Prontuario> = repository.save(prontuario)
}

class ObserveEntries(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    operator fun invoke(patientId: Long): Flow<List<ProntuarioEntry>> = repository.observeEntries(patientId)
}

class AddEntry(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun invoke(entry: ProntuarioEntry): Result<ProntuarioEntry> = repository.addEntry(entry)
}

class UpdateEntry(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun invoke(entry: ProntuarioEntry): Result<ProntuarioEntry> = repository.updateEntry(entry)
}

class DeleteEntry(private val repository: ProntuarioRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.deleteEntry(id)
}

class ProntuarioUseCases(
    val getProntuario: GetProntuario,
    val saveProntuario: SaveProntuario,
    val observeEntries: ObserveEntries,
    val addEntry: AddEntry,
    val updateEntry: UpdateEntry,
    val deleteEntry: DeleteEntry
) {
    constructor(repository: ProntuarioRepository, dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        getProntuario = GetProntuario(repository, dispatcher),
        saveProntuario = SaveProntuario(repository, dispatcher),
        observeEntries = ObserveEntries(repository, dispatcher),
        addEntry = AddEntry(repository, dispatcher),
        updateEntry = UpdateEntry(repository, dispatcher),
        deleteEntry = DeleteEntry(repository, dispatcher)
    )
}
