package com.example.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header
import com.example.domain.Patient
import com.example.BuildConfig

// Supabase REST Client
interface SupabaseApi {
    @GET("rest/v1/patients?select=*")
    suspend fun getPatients(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<PatientDto>

    @POST("rest/v1/patients")
    suspend fun createPatient(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body patient: PatientDto
    )
}

data class PatientDto(
    val id: String,
    val name: String,
    val state: String,
    val risk_flag: Boolean
)

object SupabaseNetwork {
    private val BASE_URL = BuildConfig.SUPABASE_URL + "/"
    private val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}
