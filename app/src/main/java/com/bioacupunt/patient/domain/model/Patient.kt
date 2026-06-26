package com.bioacupunt.patient.domain.model

data class Patient(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val document: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val status: String
)
