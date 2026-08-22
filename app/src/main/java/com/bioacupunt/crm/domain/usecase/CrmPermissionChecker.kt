package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.crm.domain.model.CrmPermission
import com.bioacupunt.crm.domain.model.CrmRole

/**
 * Enforces role-based access control for CRM operations.
 *
 * Each role has a specific set of permissions.
 * Before any CRM operation, check if the current user has the required permission.
 *
 * This is NOT a suggestion — it's enforcement.
 * Every repository, use case, and UI should call check() before sensitive operations.
 */
class CrmPermissionChecker {

    /**
     * Role → Permissions mapping.
     * Defines what each role can do.
     */
    private val rolePermissions: Map<CrmRole, Set<CrmPermission>> = mapOf(
        CrmRole.OWNER to CrmPermission.values().toSet(), // Owner can do everything
        CrmRole.ADMIN to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.EDIT_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.EDIT_CRM,
            CrmPermission.MANAGE_PIPELINES,
            CrmPermission.MANAGE_WORKFLOWS,
            CrmPermission.MANAGE_USERS,
            CrmPermission.EXPORT_DATA,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.CREATE_TASK,
            CrmPermission.COMPLETE_TASK,
            CrmPermission.VIEW_SEARCH,
            CrmPermission.MANAGE_VIEWS,
        ),
        CrmRole.PRACTITIONER to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.EDIT_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.EDIT_CRM,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.CREATE_TASK,
            CrmPermission.COMPLETE_TASK,
            CrmPermission.VIEW_SEARCH,
        ),
        CrmRole.ASSISTANT to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.EDIT_CRM,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.CREATE_TASK,
            CrmPermission.COMPLETE_TASK,
            CrmPermission.VIEW_SEARCH,
        ),
        CrmRole.RECEPTION to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.CREATE_TASK,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.VIEW_SEARCH,
        ),
        CrmRole.BILLING to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.EXPORT_DATA,
            CrmPermission.VIEW_SEARCH,
        ),
        CrmRole.RESEARCHER to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.VIEW_SEARCH,
        ),
        CrmRole.READ_ONLY to setOf(
            CrmPermission.VIEW_PATIENT,
            CrmPermission.VIEW_CRM,
            CrmPermission.VIEW_TIMELINE,
            CrmPermission.VIEW_SEARCH,
        ),
    )

    /**
     * Check if a role has a specific permission.
     *
     * @return true if allowed, false if denied
     */
    fun hasPermission(role: CrmRole, permission: CrmPermission): Boolean {
        val permissions = rolePermissions[role] ?: emptySet()
        return permission in permissions
    }

    /**
     * Check if a role has ALL of the specified permissions.
     */
    fun hasAllPermissions(role: CrmRole, vararg permissions: CrmPermission): Boolean {
        return permissions.all { hasPermission(role, it) }
    }

    /**
     * Get all permissions for a role.
     */
    fun getPermissions(role: CrmRole): Set<CrmPermission> {
        return rolePermissions[role] ?: emptySet()
    }

    /**
     * Verify access and return result.
     * Use this in repositories and use cases to gate operations.
     */
    fun verifyAccess(
        role: CrmRole,
        permission: CrmPermission,
        resourceType: String = "",
        resourceId: Long? = null,
    ): AccessResult {
        return if (hasPermission(role, permission)) {
            AccessResult.Allowed
        } else {
            AccessResult.Denied(
                reason = "Role ${role.label} does not have permission ${permission.label}",
                role = role,
                permission = permission,
                resourceType = resourceType,
                resourceId = resourceId,
            )
        }
    }
}

/**
 * Result of an access check.
 */
sealed class AccessResult {
    data object Allowed : AccessResult()
    data class Denied(
        val reason: String,
        val role: CrmRole,
        val permission: CrmPermission,
        val resourceType: String = "",
        val resourceId: Long? = null,
    ) : AccessResult()

    val isAllowed: Boolean get() = this is Allowed
    val isDenied: Boolean get() = this is Denied
}
