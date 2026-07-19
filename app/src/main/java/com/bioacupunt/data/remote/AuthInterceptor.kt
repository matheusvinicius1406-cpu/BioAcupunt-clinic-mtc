package com.bioacupunt.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * O token vem de [com.bioacupunt.security.SecurePreferences] (leitura síncrona de
 * SharedPreferences criptografado), então [tokenProvider] é deliberadamente
 * **não suspenso**: antes era `suspend` e forçava um `runBlocking` que bloqueava a
 * thread do dispatcher OkHttp a cada requisição, sem nenhum ganho.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runCatching { tokenProvider() }.getOrDefault("")
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
