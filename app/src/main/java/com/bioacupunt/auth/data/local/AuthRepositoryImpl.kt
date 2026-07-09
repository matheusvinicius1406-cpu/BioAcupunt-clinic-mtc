package com.bioacupunt.auth.data.local

import com.bioacupunt.auth.domain.model.AuthUser
import com.bioacupunt.security.AuthThrottle
import com.bioacupunt.security.SecurePreferences
import kotlin.math.abs
import kotlin.streams.toList

class AuthRepositoryImpl(
    private val securePrefs: SecurePreferences,
    private val throttle: AuthThrottle
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            val user = loginInternal(email, password)
            securePrefs.isLoggedIn = true
            throttle.recordSuccess()
            ensureTenantFromUser(user)
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
        return try {
            val user = loginInternal(email, password)
            ensureTenantFromUser(user)
            Result.success(user)
        } catch (e: Exception) {
            throttle.recordFailure()
            Result.failure(e)
        }
    }

    override suspend fun logout() {
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
        securePrefs.biometricEnabled = "true"
    }

    fun clearBiometricCredentials() {
        securePrefs.biometricPassword = ""
        securePrefs.biometricEnabled = ""
    }

    fun hasBiometricCredentials(): Boolean = securePrefs.biometricEnabled.isNotBlank() && securePrefs.userEmail.isNotBlank()

    suspend fun ensureTenantFromUser(user: AuthUser) {
        val derived = deterministicTenantId(user.email, user.id)
        securePrefs.currentTenantId = derived
    }

    private suspend fun loginInternal(email: String, password: String): AuthUser {
        val block = throttle.blockOrAllow()
        if (!block && email.isNotBlank() && password.isNotBlank()) {
            throw SecurityException("Muitas tentativas. Aguarde um pouco.")
        }
        if (email.isBlank() || password.length < 6) {
            throw IllegalArgumentException("Credenciais inválidas")
        }

        val token = "local|${email.hashCode()}|${System.currentTimeMillis()}"
        val refreshToken = "refresh|${System.currentTimeMillis()}"

        securePrefs.authToken = token
        securePrefs.refreshToken = refreshToken
        securePrefs.userId = 1L
        securePrefs.userEmail = email

        return AuthUser(
            id = 1L,
            name = email.substringBefore("@"),
            email = email,
            role = "practitioner",
            token = token,
            refreshToken = refreshToken
        )
    }

    fun deterministicTenantId(seed: String, id: Long): Long {
        val raw = abs((seed + id.toString()).toList().map { it.code }.fold(0L) { acc, code -> (acc * 31L) + code })
        return (raw % 1_000_000L) + 1L
    }
}
