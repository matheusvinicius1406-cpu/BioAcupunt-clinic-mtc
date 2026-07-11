package com.bioacupunt.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: suspend () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runCatching { runBlocking { tokenProvider() } }.getOrDefault("")
        val request = if (token.isBlank()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
