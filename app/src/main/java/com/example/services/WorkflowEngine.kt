package com.example.services

import com.example.core.DomainEvent
import com.example.core.PatientId
import com.example.core.EventBus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.util.UUID

enum class WorkflowStatus {
    IDLE, TRIGGERED, RUNNING, COMPLETED, FAILED
}

data class WorkflowTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val assignedTo: String,
    val completed: Boolean = false
)

data class ClinicNotification(
    val id: String = UUID.randomUUID().toString(),
    val recipient: String,
    val message: String,
    val channel: String, // "WHATSAPP", "EMAIL", "PUSH"
    val sent: Boolean = false
)

data class AutomatedWorkflow(
    val id: String = UUID.randomUUID().toString(),
    val triggerEventName: String,
    val status: WorkflowStatus = WorkflowStatus.IDLE,
    val tasksToCreate: List<WorkflowTask> = emptyList(),
    val notificationsToSend: List<ClinicNotification> = emptyList()
) {
    fun trigger(): AutomatedWorkflow {
        return this.copy(status = WorkflowStatus.TRIGGERED)
    }

    fun complete(): AutomatedWorkflow {
        return this.copy(status = WorkflowStatus.COMPLETED)
    }
}

class WorkflowEngine {
    private val activeWorkflows = mutableListOf<AutomatedWorkflow>()
    private val taskList = mutableListOf<WorkflowTask>()
    private val notificationList = mutableListOf<ClinicNotification>()

    fun registerWorkflow(workflow: AutomatedWorkflow) {
        activeWorkflows.add(workflow)
    }

    fun getTasks(): List<WorkflowTask> = taskList.toList()
    fun getNotifications(): List<ClinicNotification> = notificationList.toList()

    // Engine Automation Loop: Processes incoming DomainEvents and fires matching Workflows
    suspend fun processDomainEvent(event: DomainEvent) {
        val eventName = event::class.simpleName ?: return
        val matching = activeWorkflows.filter { it.triggerEventName == eventName }

        matching.forEach { wf ->
            // Trigger Workflow State Machine
            val triggeredWf = wf.trigger()
            
            // 1. Create clinical tasks
            wf.tasksToCreate.forEach { t ->
                val newTask = t.copy(id = UUID.randomUUID().toString())
                taskList.add(newTask)
            }

            // 2. Queue notifications
            wf.notificationsToSend.forEach { n ->
                val newNotif = n.copy(id = UUID.randomUUID().toString(), sent = true)
                notificationList.add(newNotif)
            }

            // Move to COMPLETED status
            triggeredWf.complete()
        }
    }
}
