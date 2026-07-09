package com.bioacupunt.data.remote

import com.bioacupunt.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var tenantInterceptor: TenantInterceptor

    fun init(
        tokenProvider: suspend () -> String,
        tenantProvider: suspend () -> Long?
    ) {
        authInterceptor = AuthInterceptor(tokenProvider)
        tenantInterceptor = TenantInterceptor(tenantProvider)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)
            .addInterceptor(tenantInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val api: PatientApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(PatientApi::class.java)
    }

    val appointmentApi: AppointmentApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AppointmentApi::class.java)
    }
}

private class TenantInterceptor(
    private val tenantProvider: suspend () -> Long?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val tenantId = runCatching { tenantProvider() }.getOrNull()

        val request = if (tenantId == null) {
            original
        } else {
            original.newBuilder()
                .header("X-Tenant-Id", tenantId.toString())
                .build()
        }

        return chain.proceed(request)
    }
}
