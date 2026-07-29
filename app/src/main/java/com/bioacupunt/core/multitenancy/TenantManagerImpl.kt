package com.bioacupunt.core.multitenancy

import com.bioacupunt.security.SecurePreferences

class TenantManagerImpl(private val securePreferences: SecurePreferences) : TenantManager {

    override fun currentTenantId(defaultTenantId: Long): Long {
        val stored = runCatching { securePreferences.currentTenantId }.getOrNull()
        return if (stored == null || stored <= 0) {
            setCurrentTenantId(defaultTenantId)
            defaultTenantId
        } else {
            stored
        }
    }

    override fun setCurrentTenantId(tenantId: Long) {
        securePreferences.currentTenantId = tenantId
    }

    override fun requireTenantId(defaultTenantId: Long): Long {
        val current = securePreferences.currentTenantId
        if (current == null || current <= 0) {
            setCurrentTenantId(defaultTenantId)
            return defaultTenantId
        }
        return current
    }

    override fun clear() {
        securePreferences.currentTenantId = null
    }
}
