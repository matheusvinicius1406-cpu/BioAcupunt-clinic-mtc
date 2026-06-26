package com.bioacupunt.sync

data class SyncEnvelope(
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String
)
