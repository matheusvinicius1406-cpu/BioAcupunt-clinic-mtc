package com.bioacupunt.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RefreshRequest(@Json(name = "refresh_token") val refreshToken: String)

@JsonClass(generateAdapter = true)
data class TokenPairResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer"
)

@JsonClass(generateAdapter = true)
data class AuthUserResponse(
    val id: Long,
    @Json(name = "clinic_id") val clinicId: Long,
    val email: String,
    @Json(name = "full_name") val fullName: String,
    val role: String
)

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): TokenPairResponse

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenPairResponse

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshRequest)

    @GET("/api/v1/auth/me")
    suspend fun me(): AuthUserResponse
}
