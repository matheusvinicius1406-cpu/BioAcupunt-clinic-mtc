package com.bioacupunt.data.remote

import com.bioacupunt.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // Default backend the app talks to out of the box: the deployed production
    // API. A fresh install works with no configuration. This is only the base
    // URL — never a secret; all server secrets stay on the backend. Advanced
    // users can override it in Settings > Sistema > Servidor (e.g. point at a
    // local backend during development), and HostSelectionInterceptor rewrites
    // each request's host/scheme/port accordingly without a rebuild.
    private const val DEFAULT_SERVER_URL = "https://bioacupunt-api.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var hostInterceptor: HostSelectionInterceptor

    /**
     * @param serverUrlProvider returns the base server URL (e.g. the deployed
     *   HTTPS URL) chosen by the user; blank falls back to the local default.
     */
    fun init(
        tokenProvider: suspend () -> String,
        serverUrlProvider: () -> String
    ) {
        authInterceptor = AuthInterceptor(tokenProvider)
        hostInterceptor = HostSelectionInterceptor {
            serverUrlProvider().ifBlank { DEFAULT_SERVER_URL }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(hostInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_SERVER_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val api: PatientApi by lazy { retrofit.create(PatientApi::class.java) }
}

/**
 * Rewrites each request's scheme/host/port to the currently-configured server,
 * so a single Retrofit instance can target a runtime-selectable backend
 * (OkHttp's documented dynamic-base-URL pattern).
 */
private class HostSelectionInterceptor(
    private val serverUrlProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val target = serverUrlProvider().toHttpUrlOrNull()
            ?: return chain.proceed(chain.request())
        val request = chain.request()
        val newUrl = request.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
