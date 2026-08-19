package com.bioacupunt.clinic.domain.repository

import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.TimelineEventType

/**
 * Repository that aggregates clinical events into a unified timeline.
 * Combines encounters, observations, assessments, treatments, notes, and follow-ups.
 */
interface ClinicalTimelineRepository {
    /** Get all timeline events for a patient, ordered by timestamp descending. */
    suspend fun getTimeline(patientId: Long): List<ClinicalTimelineEvent>

    /** Get timeline events filtered by type. */
    suspend fun getTimelineByType(patientId: Long, type: TimelineEventType): List<ClinicalTimelineEvent>

    /** Get timeline events within a date range. */
    suspend fun getTimelineByDateRange(patientId: Long, from: String, to: String): List<ClinicalTimelineEvent>

    /** Get the most recent N events. */
    suspend fun getRecentEvents(patientId: Long, limit: Int): List<ClinicalTimelineEvent>

    /** Get events for a specific encounter. */
    suspend fun getEventsByEncounter(encounterId: Long): List<ClinicalTimelineEvent>
}
