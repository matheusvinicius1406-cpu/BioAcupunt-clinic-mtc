package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.FollowUp
import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.InactiveReason
import com.bioacupunt.crm.domain.model.PatientOperationalAssessment
import com.bioacupunt.crm.domain.model.PatientOperationalStatus

/**
 * Determines patient operational status based on activity timestamps.
 *
 * This is an OPERATIONAL classification, not a clinical assessment.
 * ACTIVE = recent encounters/appointments
 * AT_RISK = moderate gap
 * INACTIVE = long gap or no activity
 *
 * Thresholds are configurable and documented.
 */
class InactivePatientEngine(
    private val activeDaysThreshold: Int = 30,
    private val atRiskDaysThreshold: Int = 60,
) {

    /**
     * Assess patient operational status from available activity data.
     */
    fun assess(
        patientId: Long,
        tenantId: Long,
        encounters: List<ClinicalTimelineEvent> = emptyList(),
        followUps: List<FollowUp> = emptyList(),
        activities: List<CrmActivity> = emptyList(),
        appointmentDates: List<String> = emptyList(),
    ): PatientOperationalAssessment {
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60 * 60 * 1000

        val lastEncounter = encounters.maxByOrNull { it.date }
        val lastAppointment = appointmentDates.maxOrNull()
        val lastFollowUp = followUps.maxByOrNull { it.scheduledAt }
        val lastActivity = activities.maxByOrNull { it.timestamp }

        val lastEncounterMs = lastEncounter?.date?.toLongOrNull() ?: 0L
        val lastAppointmentMs = lastAppointment?.toLongOrNull() ?: 0L
        val lastFollowUpMs = lastFollowUp?.scheduledAt?.toLongOrNull() ?: 0L
        val lastActivityMs = lastActivity?.timestamp?.toLongOrNull() ?: 0L

        val daysSinceEncounter = if (lastEncounterMs > 0) ((now - lastEncounterMs) / msPerDay).toInt() else null
        val daysSinceAppointment = if (lastAppointmentMs > 0) ((now - lastAppointmentMs) / msPerDay).toInt() else null

        val overdueFollowUps = followUps.count { it.status == com.bioacupunt.clinic.domain.model.FollowUpStatus.SCHEDULED }

        // Determine status
        val recentEventMs = listOfNotNull(
            lastEncounterMs, lastAppointmentMs, lastFollowUpMs, lastActivityMs
        ).maxOrNull() ?: 0L

        val daysSinceRecentEvent = if (recentEventMs > 0) ((now - recentEventMs) / msPerDay).toInt() else Int.MAX_VALUE

        val (status, reason) = when {
            encounters.isEmpty() && appointmentDates.isEmpty() -> {
                PatientOperationalStatus.INACTIVE to InactiveReason.NO_ENCOUNTERS
            }
            daysSinceRecentEvent > atRiskDaysThreshold -> {
                PatientOperationalStatus.INACTIVE to InactiveReason.NO_RECENT_ENCOUNTER
            }
            daysSinceRecentEvent > activeDaysThreshold -> {
                PatientOperationalStatus.AT_RISK to InactiveReason.NO_RECENT_ENCOUNTER
            }
            overdueFollowUps > 0 -> {
                PatientOperationalStatus.AT_RISK to InactiveReason.FOLLOW_UP_OVERDOWN
            }
            else -> {
                PatientOperationalStatus.ACTIVE to null
            }
        }

        return PatientOperationalAssessment(
            patientId = patientId,
            tenantId = tenantId,
            status = status,
            reason = reason,
            lastEncounterDate = lastEncounter?.date,
            lastAppointmentDate = lastAppointment,
            lastFollowUpDate = lastFollowUp?.scheduledAt,
            lastActivityDate = lastActivity?.timestamp,
            daysSinceLastEncounter = daysSinceEncounter,
            daysSinceLastAppointment = daysSinceAppointment,
            overdueFollowUps = overdueFollowUps,
        )
    }
}
