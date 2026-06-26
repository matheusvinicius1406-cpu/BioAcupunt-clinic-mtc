package com.bioacupunt.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class PatientCreateRequest(
    val tenantId: String,
    val fullName: String,
    val birthDate: String? = null,
    val gender: String? = null
)

data class PatientResponse(
    val id: Long,
    val tenantId: String,
    val fullName: String,
    val birthDate: String? = null,
    val gender: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class SyncPatientRequest(
    val entityId: String,
    val operation: String,
    val payloadJson: String
)

data class SyncPatientResponse(
    val ok: Boolean,
    val remoteId: String?
)

interface PatientApi {
    @POST("/api/patients/")
    suspend fun create(@Body request: PatientCreateRequest): PatientResponse

    @GET("/api/patients/")
    suspend fun list(): List<PatientResponse>

    @POST("/api/patients/sync/")
    suspend fun syncPatient(@Body request: SyncPatientRequest): SyncPatientResponse
}
