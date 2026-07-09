package com.bioacupunt.data.remote.model

data class SyncAppointmentRequest(
    val tenantId: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String
)
