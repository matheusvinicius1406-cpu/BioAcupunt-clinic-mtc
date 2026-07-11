package com.bioacupunt.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class PatientCreateRequest(
    val name: String,
    val document: String? = null
)

@JsonClass(generateAdapter = true)
data class PatientResponse(
    val id: Long,
    @Json(name = "clinic_id") val clinicId: Long,
    val name: String,
    val status: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

interface PatientApi {
    @POST("/api/v1/patients")
    suspend fun create(@Body request: PatientCreateRequest): PatientResponse

    @GET("/api/v1/patients")
    suspend fun list(): List<PatientResponse>
}
