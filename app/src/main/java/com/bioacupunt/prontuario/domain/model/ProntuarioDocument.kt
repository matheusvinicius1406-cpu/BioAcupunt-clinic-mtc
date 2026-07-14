package com.bioacupunt.prontuario.domain.model

/** A file attached to the patient's chart. [uri] is a persisted content:// URI. */
data class ProntuarioDocument(
    val id: Long = 0,
    val patientId: Long,
    val name: String,
    val uri: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val addedAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)
