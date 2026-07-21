package com.bioacupunt.ai.gemini

import com.bioacupunt.observability.AppLogger
import com.bioacupunt.cache.AppCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Motor central do Gemini — agnóstico de modelo.
 *
 * Aceita QUALQUER modelo Gemini (a URL leva o id informado). [listModels] descobre os
 * modelos reais da chave via endpoint oficial; [probeModel]/[workingModels] testam a
 * conexão e mantêm só os que respondem — "o que funcionar, permanece". Todos os agentes
 * de IA passam por aqui. Respostas são cacheadas em memória + disco.
 */
object GeminiEngine {

    private const val TAG = "GeminiEngine"
    /** Usado só quando o chamador não informa um modelo. */
    const val DEFAULT_MODEL = "gemini-2.5-flash"
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
        cache: AppCacheManager? = null,
        model: String = DEFAULT_MODEL
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
            val url = "$BASE_URL/${normalizeModel(model)}:generateContent?key=$apiKey"
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
            com.bioacupunt.observability.AppLogger.e("GeminiEngine", "generate failed", e)
            Result.failure(e)
        }
    }

    /** Um modelo do Gemini como o [ListModels] o descreve. */
    data class GeminiModel(
        val id: String,               // "gemini-2.5-flash" (sem o prefixo "models/")
        val displayName: String,
        val inputTokenLimit: Int,
        val supportsGenerateContent: Boolean,
    )

    /**
     * Lista TODOS os modelos que a chave da médica enxerga, direto do endpoint oficial
     * [ListModels]. Não inventamos catálogo fixo: o que a conta dela tem acesso é o que
     * aparece. Filtra para os que suportam `generateContent` (os úteis para chat).
     */
    suspend fun listModels(apiKey: String): Result<List<GeminiModel>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$BASE_URL?key=$apiKey&pageSize=1000").get().build()
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")
                val root = json.parseToJsonElement(raw).jsonObject
                val models = root["models"]?.jsonArray ?: return@use emptyList()
                models.mapNotNull { el ->
                    val o = el.jsonObject
                    val name = o["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val id = name.substringAfterLast('/')
                    if (!id.startsWith("gemini")) return@mapNotNull null
                    val methods = o["supportedGenerationMethods"]?.jsonArray
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    GeminiModel(
                        id = id,
                        displayName = o["displayName"]?.jsonPrimitive?.contentOrNull ?: id,
                        inputTokenLimit = o["inputTokenLimit"]?.jsonPrimitive?.intOrNull ?: 0,
                        supportsGenerateContent = "generateContent" in methods,
                    )
                }.filter { it.supportsGenerateContent }
            }
        }.onFailure { AppLogger.e(TAG, "listModels falhou", it) }
    }

    /**
     * Testa a conexão real com UM modelo: manda um "ping" mínimo e vê se responde.
     * Uma chave pode ver um modelo no catálogo mas não ter permissão de o executar
     * (cota, região, tier) — só o probe diz a verdade.
     */
    suspend fun probeModel(apiKey: String, model: String): Boolean =
        generate(
            apiKey = apiKey,
            prompt = "ping",
            maxTokens = 1,
            model = model,
        ).isSuccess

    /**
     * Descobre os modelos e testa cada um; devolve só os que de fato responderam.
     * "O que funcionar, permanece." Probes rodam em paralelo para não somar latências.
     */
    suspend fun workingModels(apiKey: String): Result<List<GeminiModel>> = withContext(Dispatchers.IO) {
        listModels(apiKey).mapCatching { catalog ->
            coroutineScope {
                catalog.map { m -> async { m to probeModel(apiKey, m.id) } }
                    .awaitAll()
                    .filter { it.second }
                    .map { it.first }
            }
        }
    }

    /** Aceita "gemini-2.5-flash" ou "models/gemini-2.5-flash"; a URL usa a forma curta. */
    private fun normalizeModel(model: String): String =
        model.substringAfterLast('/').ifBlank { DEFAULT_MODEL }

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
