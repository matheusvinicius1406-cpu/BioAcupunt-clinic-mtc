package com.bioacupunt.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: Long = 0L,
    val name: String = "",
    val email: String = "",
    val role: String = "practitioner",
    val token: String = "",
    val refreshToken: String = ""
)
