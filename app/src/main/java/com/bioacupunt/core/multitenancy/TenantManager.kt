package com.bioacupunt.core.multitenancy

import com.bioacupunt.security.SecurePreferences

class TenantManager(private val securePreferences: SecurePreferences) {

    fun currentTenantId(defaultTenantId: Long = 1L): Long {
        val stored = runCatching { securePreferences.currentTenantId }.getOrNull()
        return if (stored == null || stored <= 0) {
            setCurrentTenantId(defaultTenantId)
            defaultTenantId
        } else {
            stored
        }
    }

    fun setCurrentTenantId(tenantId: Long) {
        securePreferences.currentTenantId = tenantId
    }

    fun requireTenantId(defaultTenantId: Long = 1L): Long {
        val current = securePreferences.currentTenantId
        if (current == null || current <= 0) {
            setCurrentTenantId(defaultTenantId)
            return defaultTenantId
        }
        return current
    }

    fun clear() {
        securePreferences.currentTenantId = null
    }
}
