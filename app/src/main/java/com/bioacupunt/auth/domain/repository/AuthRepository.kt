package com.bioacupunt.auth.domain.repository

import com.bioacupunt.auth.domain.model.AuthUser

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun biometricLogin(): Result<AuthUser>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): AuthUser?
}
