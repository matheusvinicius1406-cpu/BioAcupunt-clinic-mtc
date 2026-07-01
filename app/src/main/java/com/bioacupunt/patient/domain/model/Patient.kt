package com.bioacupunt.patient.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Long = 0L,
    val tenantId: Long = 1L,
    val name: String,
    val document: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val status: String = "ACTIVE"
)
