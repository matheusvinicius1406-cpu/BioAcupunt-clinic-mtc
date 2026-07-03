package com.bioacupunt.auth.data.local

import com.bioacupunt.auth.domain.model.AuthUser
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.security.SecurePreferences
import com.bioacupunt.security.AuthThrottle
import java.security.MessageDigest

class AuthRepositoryImpl(
    private val securePrefs: SecurePreferences,
    private val throttle: AuthThrottle
) {

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            val user = loginInternal(email, password)
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
        return try {
            val user = loginInternal(email, password)
            Result.success(user)
        } catch (e: Exception) {
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

    private suspend fun loginInternal(email: String, password: String): AuthUser {
        val block = throttle.blockOrAllow()
        if (!block && email.isNotBlank() && password.isNotBlank()) {
            throw SecurityException("Muitas tentativas. Aguarde um pouco.")
        }
        if (email.isBlank() || password.length < 6) {
            throw IllegalArgumentException("Credenciais inválidas")
        }
        val user = AuthUser(
            id = 1L,
            name = "Dra. Camila",
            email = email,
            role = "practitioner",
            token = generateDemoToken(email),
            refreshToken = "refresh_${System.currentTimeMillis()}"
        )
        securePrefs.authToken = user.token
        securePrefs.refreshToken = user.refreshToken
        securePrefs.userId = user.id
        securePrefs.userEmail = user.email
        return user
    }

    private fun generateDemoToken(email: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest("$email${System.currentTimeMillis()}".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
