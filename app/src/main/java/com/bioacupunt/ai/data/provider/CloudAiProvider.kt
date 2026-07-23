package com.bioacupunt.ai.data.provider

import com.bioacupunt.ai.config.AiConfigManager
import com.bioacupunt.ai.config.AiSecretsProvider
import com.bioacupunt.ai.core.AiCapability
import com.bioacupunt.ai.core.AiExecutionType
import com.bioacupunt.ai.core.AiModelDescriptor
import com.bioacupunt.ai.core.AiPricingModel
import com.bioacupunt.ai.core.AiProvider
import com.bioacupunt.ai.core.AiProviderCapabilities
import com.bioacupunt.ai.core.AiProviderMetadata
import com.bioacupunt.ai.core.AiRequest
import com.bioacupunt.ai.core.AiResult
import com.bioacupunt.ai.core.DevicePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * OPTIONAL CLOUD LLM PROVIDER — off by default, offline-first stays the rule.
 *
 * The product decision is "Gemma on-device (padrão) + nuvem OPCIONAL que a médica liga
 * manualmente". This provider is that opt-in cloud path. It is always *registered* in the
 * orchestrator, but [isAvailable] returns false unless BOTH conditions hold:
 *
 *   1. the doctor turned cloud on ([AiConfigManager.isCloudEnabled]); and
 *   2. an API key exists ([AiSecretsProvider.apiKeyFor]).
 *
 * Until then the orchestrator never routes here, so on a fresh install with no local
 * model and cloud off, the app returns `NoProviderAvailable` — degraded, honest, and with
 * no patient data leaving the device.
 *
 * ## LGPD boundary
 *
 * A patient chart is *dado pessoal sensível* (LGPD Art. 5º, II). Sending it to a remote
 * API is a processing decision that needs the doctor's explicit consent. That consent is
 * exactly the enable toggle above: this provider only ever runs after she flips it. The UI
 * that flips it MUST carry the LGPD warning that clinical text will leave the device.
 *
 * ## What it must NEVER touch (R1 / R2)
 *
 *   - Clinical safety screening never calls any LLM — it is deterministic Kotlin in
 *     `ClinicalSafetyEngine`. This provider, like every provider, is invisible to it.
 *   - The R2 gate (`if (!grounding.hasEvidence) return NoEvidence`) runs in
 *     `AskLibraryUseCase` *before* any provider is reached. Cloud serves only the two
 *     paths that already survived that gate: grounded RAG and the free fallback chat.
 *
 * ## Preference vs local
 *
 * Whenever the local model is available it is preferred (see the local-boosting scorer
 * wired in `AppContainer`), so cloud is a genuine fallback, not the default.
 */
class CloudAiProvider(
    private val configManager: AiConfigManager,
    private val secretsProvider: AiSecretsProvider,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS)
        .build(),
) : AiProvider {

    override val id: String = PROVIDER_ID

    // Reuses the "gemini" secret slot so the existing SecurePreferences.geminiApiKey
    // field (read by AndroidAiSecretsProvider) is the key source — no new secret plumbing.
    private val secretId: String = "gemini"

    override val displayName: String = "IA na nuvem (Gemini)"

    override val capabilities = AiProviderCapabilities(
        capabilities = setOf(AiCapability.Chat),
        maxContextTokens = MAX_CONTEXT_TOKENS,
        supportsStream = false,
        supportsLocalExecution = false,
        preferredDevice = DevicePreference.Auto,
    )

    override val metadata = AiProviderMetadata(
        providerType = PROVIDER_ID,
        executionType = AiExecutionType.Remote,
        pricingModel = AiPricingModel.PerToken,
        estimatedCostPer1kTokens = 0.0,
        supportsOffline = false,
        supportsStreaming = false,
        supportsLongContext = true,
        maxContextWindow = MAX_CONTEXT_TOKENS,
        maxOutputTokens = MAX_OUTPUT_TOKENS,
        recommendedDeviceClass = DevicePreference.Auto,
        minimumAndroidVersion = 24,
        hardwareAcceleration = "cloud",
    )

    override val models: List<AiModelDescriptor> = listOf(
        AiModelDescriptor(
            id = MODEL_ID,
            providerId = PROVIDER_ID,
            displayName = "Gemini 2.5 Flash",
            capabilities = setOf(AiCapability.Chat),
            contextTokens = MAX_CONTEXT_TOKENS,
            isLocal = false,
            // Numerically behind the local model; the local-boosting scorer in
            // AppContainer additionally guarantees local wins whenever it is available.
            fallbackOrder = 50,
            metadata = metadata,
        ),
    )

    /** Off by default; only truthy once the doctor enabled cloud AND a key is present. */
    override suspend fun isAvailable(): Boolean {
        if (!configManager.isCloudEnabled()) return false
        val key = runCatching { secretsProvider.apiKeyFor(secretId) }.getOrNull()
        return !key.isNullOrBlank()
    }

    override suspend fun generate(request: AiRequest): Result<AiResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Re-check the gate here, not just in isAvailable(): a caller that reaches
                // generate() directly must still not leak data when cloud is disabled.
                check(configManager.isCloudEnabled()) { "IA na nuvem está desligada em Ajustes > IA." }
                val apiKey = secretsProvider.apiKeyFor(secretId)?.takeIf { it.isNotBlank() }
                    ?: error("Chave de API da nuvem não configurada em Ajustes > IA.")

                val started = System.currentTimeMillis()
                val text = withTimeout(REQUEST_TIMEOUT_MS) {
                    callGemini(apiKey, request)
                }.trim()
                check(text.isNotEmpty()) { "A nuvem devolveu resposta vazia." }

                AiResult(
                    text = text,
                    providerId = PROVIDER_ID,
                    modelId = MODEL_ID,
                    capabilitiesUsed = setOf(AiCapability.Chat),
                    latencyMs = System.currentTimeMillis() - started,
                    decisionReason = "Executado na nuvem (habilitado pela médica). Texto saiu do aparelho.",
                    metadata = mapOf(
                        "execution" to "cloud",
                        "dataLeftDevice" to "true",
                    ),
                )
            }.onFailure { if (it is CancellationException) throw it }
        }

    private fun callGemini(apiKey: String, request: AiRequest): String {
        val url = "$ENDPOINT/$MODEL_ID:generateContent?key=$apiKey"

        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", buildUserText(request))),
                    ),
                ),
            )
            if (request.systemPrompt.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", request.systemPrompt.trim())),
                    ),
                )
            }
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxTokens.coerceIn(1, MAX_OUTPUT_TOKENS))
                },
            )
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            check(response.isSuccessful) {
                "Falha na nuvem: HTTP ${response.code}${extractApiError(payload)?.let { " — $it" } ?: ""}"
            }
            return parseText(payload)
        }
    }

    private fun buildUserText(request: AiRequest): String = buildString {
        if (request.context.isNotEmpty()) {
            appendLine("Contexto:")
            request.context.forEach { (k, v) -> appendLine("- $k: $v") }
            appendLine()
        }
        append(request.prompt.trim())
    }

    private fun parseText(payload: String): String {
        val root = JSONObject(payload)
        val candidates = root.optJSONArray("candidates")
            ?: error("Resposta da nuvem sem candidatos.")
        if (candidates.length() == 0) error("Resposta da nuvem sem candidatos.")
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts")
            ?: error("Resposta da nuvem sem conteúdo.")
        return buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text", ""))
            }
        }
    }

    private fun extractApiError(payload: String): String? = runCatching {
        JSONObject(payload).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
    }.getOrNull()

    companion object {
        const val PROVIDER_ID = "cloud-gemini"
        const val MODEL_ID = "gemini-2.5-flash"

        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private const val MAX_CONTEXT_TOKENS = 32_768
        private const val MAX_OUTPUT_TOKENS = 2048
        private const val REQUEST_TIMEOUT_MS = 45_000L
    }
}
