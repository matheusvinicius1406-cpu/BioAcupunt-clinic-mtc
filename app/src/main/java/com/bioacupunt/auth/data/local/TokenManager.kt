package com.bioacupunt.auth.data.local

import com.bioacupunt.security.SecurePreferences

class TokenManager(private val securePreferences: SecurePreferences) {

    suspend fun getToken(): String = securePreferences.authToken.orEmpty()

    suspend fun setToken(token: String) {
        securePreferences.authToken = token
    }

    suspend fun getRefreshToken(): String = securePreferences.refreshToken.orEmpty()

    suspend fun setRefreshToken(token: String) {
        securePreferences.refreshToken = token
    }

    suspend fun clear() {
        securePreferences.authToken = ""
        securePreferences.refreshToken = ""
    }
}
