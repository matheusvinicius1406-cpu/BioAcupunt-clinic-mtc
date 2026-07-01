package com.bioacupunt.ai.gemini

import android.util.Log
import com.bioacupunt.cache.AppCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Central Gemini 2.0 Flash engine.
 * All AI agents route through here.
 * Responses are cached in memory + disk to avoid repeated API calls.
 */
object GeminiEngine {

    private const val TAG = "GeminiEngine"
    private const val MODEL = "gemini-2.0-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generate(
        apiKey: String,
        prompt: String,
        systemPrompt: String = "",
        temperature: Double = 0.7,
        maxTokens: Int = 2048,
        cacheKey: String? = null,
        cache: AppCacheManager? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (cacheKey != null && cache != null) {
                cache.get(cacheKey)?.let { return@withContext Result.success(it) }
                cache.getDisk(cacheKey)?.let { v ->
                    cache.put(cacheKey, v)
                    return@withContext Result.success(v)
                }
            }

            val body = buildBody(prompt, systemPrompt, temperature, maxTokens)
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val req = Request.Builder().url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = http.newCall(req).execute()
            val raw = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty"))
            if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}: $raw"))

            val text = extractText(raw)
            if (cacheKey != null && cache != null && text.isNotBlank()) {
                cache.put(cacheKey, text)
                cache.putDisk(cacheKey, text)
            }
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "generate failed", e)
            Result.failure(e)
        }
    }

    private fun buildBody(prompt: String, system: String, temp: Double, maxTk: Int): String {
        val parts = buildString {
            append("""{"contents":[""")
            if (system.isNotBlank()) {
                append("""{"role":"user","parts":[{"text":${Json.encodeToString(system)}}]},""")
                append("""{"role":"model","parts":[{"text":"Entendido."}]},""")
            }
            append("""{"role":"user","parts":[{"text":${Json.encodeToString(prompt)}}]}""")
            append("""],"generationConfig":{"temperature":$temp,"maxOutputTokens":$maxTk}}""")
        }
        return parts
    }

    private fun extractText(raw: String): String =
        """"text"\s*:\s*"((?:[^"\\]|\\.)*)" """.toRegex()
            .findAll(raw)
            .joinToString("") { it.groupValues[1] }
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .trim()
}
