package com.bioacupunt.auth.data.local

import com.bioacupunt.auth.domain.model.AuthUser
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.core.multitenancy.TenantManager
import com.bioacupunt.data.remote.AuthApi
import com.bioacupunt.data.remote.LoginRequest
import com.bioacupunt.data.remote.RefreshRequest
import com.bioacupunt.data.remote.TokenPairResponse
import com.bioacupunt.security.AuthThrottle
import com.bioacupunt.security.SecurePreferences

class AuthRepositoryImpl(
    private val securePrefs: SecurePreferences,
    private val throttle: AuthThrottle,
    private val authApi: AuthApi,
    private val tenantManager: TenantManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            if (!throttle.blockOrAllow()) {
                throw SecurityException("Muitas tentativas. Aguarde um pouco.")
            }
            if (email.isBlank() || password.isBlank()) {
                throw IllegalArgumentException("Informe e-mail e senha.")
            }

            val tokens = authApi.login(LoginRequest(email.trim(), password))
            val user = persistSession(tokens)
            securePrefs.isLoggedIn = true
            throttle.recordSuccess()
            Result.success(user)
        } catch (e: Exception) {
            throttle.recordFailure()
            Result.failure(e)
        }
    }

    override suspend fun biometricLogin(): Result<AuthUser> {
        if (!securePrefs.biometricEnabled) return Result.failure(IllegalStateException("Biometria não habilitada"))
        val email = securePrefs.userEmail.orEmpty()
        val password = securePrefs.biometricPassword.orEmpty()
        if (email.isBlank() || password.isBlank()) return Result.failure(IllegalStateException("Credenciais biométricas indisponíveis"))
        return login(email, password)
    }

    override suspend fun logout() {
        val refreshToken = securePrefs.refreshToken
        if (refreshToken.isNotBlank()) {
            runCatching { authApi.logout(RefreshRequest(refreshToken)) }
        }
        securePrefs.clearAll()
    }

    override fun isLoggedIn(): Boolean = securePrefs.isLoggedIn

    override fun getCurrentUser(): AuthUser? {
        if (!securePrefs.isLoggedIn) return null
        return AuthUser(
            id = securePrefs.userId,
            email = securePrefs.userEmail ?: "",
            token = securePrefs.authToken ?: ""
        )
    }

    fun setBiometricCredentials(email: String, password: String) {
        securePrefs.userEmail = email
        securePrefs.biometricPassword = password
        securePrefs.biometricEnabled = true
    }

    fun clearBiometricCredentials() {
        securePrefs.biometricPassword = ""
        securePrefs.biometricEnabled = false
    }

    fun hasBiometricCredentials(): Boolean = securePrefs.biometricEnabled && securePrefs.userEmail.isNotBlank()

    private suspend fun persistSession(tokens: TokenPairResponse): AuthUser {
        securePrefs.authToken = tokens.accessToken
        securePrefs.refreshToken = tokens.refreshToken

        // Requires the access token just stored above: AuthInterceptor reads it
        // fresh from SecurePreferences on every request.
        val me = authApi.me()
        securePrefs.userId = me.id
        securePrefs.userEmail = me.email
        tenantManager.setCurrentTenantId(me.clinicId)

        return AuthUser(
            id = me.id,
            name = me.fullName,
            email = me.email,
            role = me.role,
            token = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )
    }
}
