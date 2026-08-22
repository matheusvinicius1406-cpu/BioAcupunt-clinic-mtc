package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.crm.domain.model.CrmTask
import com.bioacupunt.crm.domain.model.CrmWorkflow
import com.bioacupunt.crm.domain.model.TaskPriority
import com.bioacupunt.crm.domain.model.TaskRelation
import com.bioacupunt.crm.domain.model.TaskStatus
import com.bioacupunt.crm.domain.model.WorkflowAction
import com.bioacupunt.crm.domain.model.WorkflowActionType
import com.bioacupunt.crm.domain.model.WorkflowCondition
import com.bioacupunt.crm.domain.model.WorkflowTriggerType
import java.time.Instant

/**
 * Executes CRM workflows based on triggers.
 *
 * Workflow flow:
 * EVENT → CONDITION → ACTION
 *
 * Safety: Workflows CANNOT:
 * - Create diagnoses
 * - Confirm observations
 * - Overwrite clinical notes
 * - Change assessments
 * - Modify clinical facts
 *
 * Each execution generates an audit event.
 */
class WorkflowExecutor(
    private val taskRepository: TaskRepository,
    private val auditLogger: CrmAuditLogger,
) {

    interface TaskRepository {
        suspend fun create(task: CrmTask): Long
    }

    /**
     * Execute a workflow when triggered.
     *
     * @param workflow The workflow to execute
     * @param triggerType What triggered this workflow
     * @param context Data about the triggering event (patientId, encounterId, etc.)
     * @return List of task IDs created by this workflow
     */
    suspend fun execute(
        workflow: CrmWorkflow,
        triggerType: WorkflowTriggerType,
        context: WorkflowContext,
    ): ExecutionResult {
        // Verify trigger matches
        if (workflow.triggerType != triggerType) {
            return ExecutionResult(
                success = false,
                reason = "Trigger mismatch: expected ${workflow.triggerType.name}, got ${triggerType.name}",
            )
        }

        // Verify workflow is active
        if (!workflow.isActive) {
            return ExecutionResult(
                success = false,
                reason = "Workflow is inactive",
            )
        }

        // Check conditions
        val conditionsMet = workflow.conditions.all { condition ->
            evaluateCondition(condition, context)
        }

        if (!conditionsMet) {
            return ExecutionResult(
                success = false,
                reason = "Conditions not met",
            )
        }

        // Execute actions
        val createdTaskIds = mutableListOf<Long>()

        for (action in workflow.actions) {
            // Safety check: ensure action is not clinical
            if (isClinicalAction(action)) {
                return ExecutionResult(
                    success = false,
                    reason = "Clinical action blocked: ${action.type.name}",
                )
            }

            when (action.type) {
                WorkflowActionType.CREATE_TASK -> {
                    val task = createTaskFromAction(action, workflow.tenantId, context)
                    val taskId = taskRepository.create(task)
                    createdTaskIds.add(taskId)
                }
                WorkflowActionType.CREATE_ACTIVITY -> {
                    // Activity creation handled by caller
                }
                WorkflowActionType.UPDATE_STATUS -> {
                    // Status update handled by caller
                }
                WorkflowActionType.SEND_NOTIFICATION -> {
                    // Notification handled by caller
                }
                WorkflowActionType.ADD_TAG -> {
                    // Tag handling handled by caller
                }
                WorkflowActionType.REMOVE_TAG -> {
                    // Tag handling handled by caller
                }
            }
        }

        // Audit the execution
        auditLogger.logWorkflowExecuted(
            tenantId = workflow.tenantId,
            userId = "system",
            workflowId = workflow.id,
            triggerType = triggerType.name,
        )

        return ExecutionResult(
            success = true,
            createdTaskIds = createdTaskIds,
            actionsExecuted = workflow.actions.size,
        )
    }

    /**
     * Evaluate a workflow condition against context.
     */
    private fun evaluateCondition(condition: WorkflowCondition, context: WorkflowContext): Boolean {
        val value = context.data[condition.field] ?: return false

        return when (condition.operator) {
            "equals" -> value == condition.value
            "not_equals" -> value != condition.value
            "contains" -> value.contains(condition.value, ignoreCase = true)
            "greater_than" -> {
                val numValue = value.toDoubleOrNull() ?: return false
                val condValue = condition.value.toDoubleOrNull() ?: return false
                numValue > condValue
            }
            "less_than" -> {
                val numValue = value.toDoubleOrNull() ?: return false
                val condValue = condition.value.toDoubleOrNull() ?: return false
                numValue < condValue
            }
            else -> false
        }
    }

    /**
     * Create a CRM task from a workflow action.
     */
    private fun createTaskFromAction(
        action: WorkflowAction,
        tenantId: Long,
        context: WorkflowContext,
    ): CrmTask {
        val title = action.params["title"] ?: "Workflow task"
        val description = action.params["description"] ?: ""
        val priority = try {
            TaskPriority.valueOf(action.params["priority"] ?: "MEDIUM")
        } catch (e: Exception) {
            TaskPriority.MEDIUM
        }

        return CrmTask(
            tenantId = tenantId,
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            priority = priority,
            category = "WORKFLOW",
            relatedEntityId = context.patientId,
            relationType = TaskRelation.PATIENT,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
        )
    }

    /**
     * Safety check: ensure action is NOT clinical.
     */
    private fun isClinicalAction(action: WorkflowAction): Boolean {
        val clinicalActions = setOf(
            "CREATE_DIAGNOSIS",
            "CONFIRM_OBSERVATION",
            "OVERWRITE_CLINICAL_NOTE",
            "CHANGE_ASSESSMENT",
            "CHANGE_CLINICAL_FACTS",
        )
        return clinicalActions.any { it in action.params.keys }
    }
}

/**
 * Context data for workflow execution.
 */
data class WorkflowContext(
    val patientId: Long? = null,
    val encounterId: Long? = null,
    val leadId: Long? = null,
    val followUpId: Long? = null,
    val data: Map<String, String> = emptyMap(),
)

/**
 * Result of workflow execution.
 */
data class ExecutionResult(
    val success: Boolean,
    val reason: String = "",
    val createdTaskIds: List<Long> = emptyList(),
    val actionsExecuted: Int = 0,
)
