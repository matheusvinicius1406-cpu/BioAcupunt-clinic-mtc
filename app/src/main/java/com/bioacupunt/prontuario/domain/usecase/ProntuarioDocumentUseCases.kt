package com.bioacupunt.prontuario.domain.usecase

import com.bioacupunt.core.util.Result
import com.bioacupunt.prontuario.domain.model.ProntuarioDocument
import com.bioacupunt.prontuario.domain.repository.ProntuarioDocumentRepository
import kotlinx.coroutines.flow.Flow

class ObserveDocuments(private val repository: ProntuarioDocumentRepository) {
    operator fun invoke(patientId: Long): Flow<List<ProntuarioDocument>> = repository.observe(patientId)
}
class SaveDocument(private val repository: ProntuarioDocumentRepository) {
    suspend operator fun invoke(document: ProntuarioDocument): Result<ProntuarioDocument> = repository.save(document)
}
class DeleteDocument(private val repository: ProntuarioDocumentRepository) {
    suspend operator fun invoke(id: Long): Result<Boolean> = repository.delete(id)
}

class ProntuarioDocumentUseCases(
    val observeDocuments: ObserveDocuments,
    val saveDocument: SaveDocument,
    val deleteDocument: DeleteDocument,
) {
    constructor(repository: ProntuarioDocumentRepository) : this(
        observeDocuments = ObserveDocuments(repository),
        saveDocument = SaveDocument(repository),
        deleteDocument = DeleteDocument(repository),
    )
}
