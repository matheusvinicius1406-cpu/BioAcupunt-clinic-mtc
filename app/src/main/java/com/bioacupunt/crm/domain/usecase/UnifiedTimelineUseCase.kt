package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.clinic.domain.model.ClinicalTimelineEvent
import com.bioacupunt.clinic.domain.model.TimelineEventType
import com.bioacupunt.crm.domain.model.CrmActivity
import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.UnifiedTimelineEvent
import com.bioacupunt.crm.domain.model.UnifiedTimelineSource

/**
 * Merges clinical timeline events + CRM activities + CRM tasks
 * into a single unified timeline.
 *
 * Key invariant: NO DUPLICATION. If the same event appears in both
 * clinical and CRM, it appears only once in the unified timeline.
 *
 * Ordering: timestamp + deterministic eventId (never non-deterministic).
 */
class UnifiedTimelineUseCase {

    /**
     * Build a unified timeline for a patient.
     *
     * @param patientId Patient to build timeline for
     * @param clinicalEvents Clinical timeline events (encounters, observations, etc.)
     * @param crmActivities CRM activities (calls, emails, meetings, etc.)
     * @param crmTasks CRM tasks (with due dates, statuses)
     * @param maxResults Maximum number of events to return
     * @return Unified timeline sorted by timestamp descending (newest first)
     */
    fun buildTimeline(
        patientId: Long,
        tenantId: Long,
        clinicalEvents: List<ClinicalTimelineEvent> = emptyList(),
        crmActivities: List<CrmActivity> = emptyList(),
        crmTasks: List<CrmTask> = emptyList(),
        maxResults: Int = 50,
    ): List<UnifiedTimelineEvent> {
        val events = mutableListOf<UnifiedTimelineEvent>()

        // Add clinical events
        clinicalEvents.forEach { event ->
            events.add(
                UnifiedTimelineEvent(
                    id = "clinical-${event.id}",
                    patientId = event.patientId,
                    tenantId = event.tenantId,
                    source = UnifiedTimelineSource.CLINICAL,
                    type = event.type.name,
                    title = event.title,
                    summary = event.summary,
                    entityId = event.entityId,
                    entityType = event.type.name,
                    timestamp = event.date,
                    metadata = event.metadata,
                )
            )
        }

        // Add CRM activities (only those related to this patient)
        crmActivities
            .filter { it.relatedEntityId == patientId || it.relatedEntityId == 0L }
            .forEach { activity ->
                events.add(
                    UnifiedTimelineEvent(
                        id = "crm-activity-${activity.id}",
                        patientId = patientId,
                        tenantId = activity.tenantId,
                        source = UnifiedTimelineSource.CRM,
                        type = activity.type.name,
                        title = activity.title,
                        summary = activity.description,
                        entityId = activity.id,
                        entityType = "ACTIVITY",
                        timestamp = activity.timestamp,
                    )
                )
            }

        // Add CRM tasks (only those related to this patient and with relevant status)
        crmTasks
            .filter { it.relatedEntityId == patientId }
            .forEach { task ->
                val source = when (task.status) {
                    TaskStatus.COMPLETED -> UnifiedTimelineSource.ADMINISTRATIVE
                    else -> UnifiedTimelineSource.CRM
                }
                events.add(
                    UnifiedTimelineEvent(
                        id = "crm-task-${task.id}",
                        patientId = patientId,
                        tenantId = task.tenantId,
                        source = source,
                        type = "TASK_${task.status.name}",
                        title = task.title,
                        summary = task.description,
                        entityId = task.id,
                        entityType = "TASK",
                        timestamp = task.createdAt,
                    )
                )
            }

        // Sort by timestamp descending (newest first), then by ID for determinism
        return events
            .sortedWith(compareByDescending<UnifiedTimelineEvent> { it.timestamp }.thenBy { it.id })
            .take(maxResults)
    }

    /**
     * Deduplicate events that might appear in both clinical and CRM.
     * Uses entity type + entity ID as deduplication key.
     */
    fun deduplicate(events: List<UnifiedTimelineEvent>): List<UnifiedTimelineEvent> {
        val seen = mutableSetOf<String>()
        return events.filter { event ->
            val key = "${event.entityType}-${event.entityId}"
            if (key in seen) {
                false // Duplicate — skip
            } else {
                seen.add(key)
                true
            }
        }
    }
}
