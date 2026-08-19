package com.bioacupunt.clinic.domain.model

/**
 * A follow-up appointment/task for a patient.
 *
 * Tracks what needs to happen next, when, and what was expected vs actual.
 */
data class FollowUp(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val encounterId: Long? = null,
    val scheduledAt: String = "",
    val reason: String = "",
    val expectedFindings: String = "",
    val actualFindings: String = "",
    val status: FollowUpStatus = FollowUpStatus.SCHEDULED,
    val completedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

enum class FollowUpStatus {
    SCHEDULED,
    COMPLETED,
    MISSED,
    CANCELLED,
}
