package com.bioacupunt.data.remote

import com.bioacupunt.data.remote.model.SyncAppointmentRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AppointmentApi {

    @POST("/api/appointments/sync/")
    suspend fun syncAppointment(@Body request: SyncAppointmentRequest): com.bioacupunt.data.remote.SyncPatientResponse
}
