package com.bioacupunt.core.domain

sealed interface AppState {
    data object Unknown : AppState
    data class Ready(
        val auth: AuthState,
        val sync: SyncState,
        val network: NetworkState,
        val theme: ThemeState,
        val settings: SettingsState,
        val user: UserState,
        val ai: AIState
    ) : AppState
}

data class AuthState(
    val isLoggedIn: Boolean = false,
    val lastLoginAt: String? = null,
    val biometricEnabled: Boolean = false
)

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncAt: String? = null,
    val pendingCount: Int = 0,
    val lastError: String? = null
)

data class NetworkState(
    val isConnected: Boolean = false,
    val isMetered: Boolean = false,
    val lastCheckedAt: String? = null
)

data class ThemeState(
    val useDarkTheme: Boolean = false,
    val useDynamicColor: Boolean = true
)

data class SettingsState(
    val defaultReminderMinutes: Int = 30,
    val lockTimeoutMinutes: Int = 1,
    val reportDefaultFormat: String = "PDF"
)

data class UserState(
    val displayName: String? = null,
    val role: String = "CLINICIAN"
)

data class AIState(
    val isEnabled: Boolean = true,
    val primaryModel: String = "gemini-default",
    val lastUsedAt: String? = null
)
