package com.bioacupunt.auth.domain.repository

import com.bioacupunt.auth.domain.model.AuthUser

interface AuthRepository {
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        clinicName: String? = null
    ): Result<AuthUser>
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun biometricLogin(): Result<AuthUser>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): AuthUser?
}
