package com.bioacupunt.auth.data.local

import com.bioacupunt.auth.domain.model.AuthUser
import com.bioacupunt.auth.domain.repository.AuthRepository
import com.bioacupunt.security.SecurePreferences
import java.security.MessageDigest

/**
 * Offline-first auth: validates against locally stored credentials.
 * In production, connect to a backend JWT endpoint.
 */
class AuthRepositoryImpl(
    private val securePrefs: SecurePreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return try {
            // Demo validation - in production: call API, verify JWT
            if (email.isBlank() || password.length < 6) {
                return Result.failure(IllegalArgumentException("Credenciais inválidas"))
            }

            // Simulate successful login for demo
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
            securePrefs.isLoggedIn = true

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

    private fun generateDemoToken(email: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest("$email${System.currentTimeMillis()}".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
