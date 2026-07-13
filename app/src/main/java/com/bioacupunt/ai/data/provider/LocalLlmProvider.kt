package com.bioacupunt.ai.data.provider

import android.content.Context
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ON-DEVICE LLM PROVIDER — Gemma running fully inside the app.
 *
 * Plugs into the existing [AiProvider] contract, so the orchestrator can route to it
 * exactly like any cloud provider. No architectural change was needed: the AI layer
 * already modelled [AiExecutionType.Local], [DevicePreference] and
 * `AiRequest.preferLocal`.
 *
 * ## Why this matters clinically, not just technically
 *
 * A patient chart is health data. Under the LGPD it is *dado pessoal sensível*
 * (Art. 5º, II), and shipping it to a third-party API is a processing decision that
 * needs a legal basis, a DPA, and — realistically — a conversation the doctor does
 * not want to have. An on-device model sidesteps the entire question: the tongue
 * photo, the pulse, the diagnosis never leave the phone. That is why local execution
 * is the *default* for anything touching a chart, and the cloud is the exception.
 *
 * ## The model is not in the APK, and cannot be
 *
 * Gemma weights are hundreds of MB to multiple GB — far past the Play Store's
 * delivery limits. The model is downloaded once, on demand, into app-private storage
 * (see [LocalModelManager]); [isAvailable] reports false until it is present, and the
 * orchestrator simply routes elsewhere in the meantime. The app must degrade, never
 * break, on a device where the model was never downloaded.
 *
 * ## Implementation note
 *
 * This targets MediaPipe's `tasks-genai` LLM Inference API, which is what ships and
 * works on Android today. Google has since put that API into maintenance mode and
 * points new work at LiteRT-LM. The blast radius of that migration is deliberately
 * confined to this one file: nothing outside it knows which runtime is underneath.
 */
class LocalLlmProvider(
    private val context: Context,
    private val modelManager: LocalModelManager,
) : AiProvider {

    override val id: String = PROVIDER_ID
    override val displayName: String = "Gemma (no dispositivo)"

    override val capabilities = AiProviderCapabilities(
        capabilities = setOf(AiCapability.Chat),
        maxContextTokens = MAX_CONTEXT_TOKENS,
        supportsStream = false,
        supportsLocalExecution = true,
        preferredDevice = DevicePreference.Auto,
    )

    override val metadata = AiProviderMetadata(
        providerType = PROVIDER_ID,
        executionType = AiExecutionType.Local,
        pricingModel = AiPricingModel.Free,
        estimatedCostPer1kTokens = 0.0,
        supportsOffline = true,
        supportsStreaming = false,
        supportsLongContext = false,
        maxContextWindow = MAX_CONTEXT_TOKENS,
        maxOutputTokens = MAX_OUTPUT_TOKENS,
        recommendedDeviceClass = DevicePreference.Auto,
        minimumAndroidVersion = 24,
        hardwareAcceleration = "CPU/GPU (LiteRT)",
    )

    override val models: List<AiModelDescriptor> = listOf(
        AiModelDescriptor(
            id = MODEL_ID,
            providerId = PROVIDER_ID,
            displayName = "Gemma 3 1B (int4)",
            capabilities = setOf(AiCapability.Chat),
            contextTokens = MAX_CONTEXT_TOKENS,
            isLocal = true,
            // Preferred over cloud whenever it can serve the request: free, offline,
            // and the only option that keeps patient data on the device.
            fallbackOrder = 0,
            metadata = metadata,
        ),
    )

    /** Engine creation is expensive (seconds) and not thread-safe: build once, guard it. */
    @Volatile
    private var engine: LlmInference? = null
    private val engineLock = Mutex()

    override suspend fun isAvailable(): Boolean = modelManager.isModelReady()

    override suspend fun generate(request: AiRequest): Result<AiResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                val modelFile = modelManager.modelFile()
                check(modelFile.exists()) {
                    "Modelo local ausente. Baixe o modelo em Ajustes > IA."
                }

                val llm = obtainEngine(modelFile)
                val startedAt = System.currentTimeMillis()

                val prompt = buildPrompt(request)
                val text = llm.generateResponse(prompt).orEmpty().trim()

                AiResult(
                    text = text,
                    providerId = PROVIDER_ID,
                    modelId = MODEL_ID,
                    capabilitiesUsed = setOf(AiCapability.Chat),
                    latencyMs = System.currentTimeMillis() - startedAt,
                    decisionReason = "Executado no dispositivo: dado clínico não saiu do aparelho.",
                    metadata = mapOf(
                        "execution" to "local",
                        "dataLeftDevice" to "false",
                    ),
                )
            }
        }

    private suspend fun obtainEngine(modelFile: File): LlmInference {
        engine?.let { return it }
        return engineLock.withLock {
            engine ?: createEngine(modelFile).also { engine = it }
        }
    }

    private fun createEngine(modelFile: File): LlmInference {
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(MAX_CONTEXT_TOKENS)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    /**
     * MediaPipe exposes a single prompt string, not a role-separated chat API, so the
     * system prompt is folded in here.
     *
     * [CLINICAL_GUARDRAIL] is prepended to every clinical call and is not overridable
     * by the caller. It is a *second* line of defence and nothing more: the real
     * contraindication screening happens in
     * [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine], deterministically,
     * before any protocol reaches the screen. A prompt can be talked out of its
     * instructions; a `when` branch cannot. Never let a guardrail string stand in for
     * the safety engine.
     */
    private fun buildPrompt(request: AiRequest): String = buildString {
        appendLine(CLINICAL_GUARDRAIL)
        if (request.systemPrompt.isNotBlank()) {
            appendLine()
            appendLine(request.systemPrompt.trim())
        }
        if (request.context.isNotEmpty()) {
            appendLine()
            appendLine("Contexto clínico:")
            request.context.forEach { (k, v) -> appendLine("- $k: $v") }
        }
        appendLine()
        append(request.prompt.trim())
    }

    /** Frees native memory. Call when the AI screen leaves the backstack. */
    suspend fun release() = engineLock.withLock {
        runCatching { engine?.close() }
        engine = null
    }

    companion object {
        const val PROVIDER_ID = "local-gemma"
        const val MODEL_ID = "gemma-3-1b-it-int4"

        private const val MAX_CONTEXT_TOKENS = 2048
        private const val MAX_OUTPUT_TOKENS = 1024

        val CLINICAL_GUARDRAIL = """
            Você é um assistente de apoio ao raciocínio clínico em Medicina Tradicional
            Chinesa, falando com uma profissional habilitada.

            Regras invioláveis:
            - Você APOIA o raciocínio da profissional. Você NUNCA substitui o julgamento
              clínico dela, e nunca apresenta uma conclusão como definitiva.
            - Você NÃO valida contraindicações. A triagem de segurança é feita por um
              motor determinístico do aplicativo, fora de você.
            - Se faltar informação para raciocinar, diga o que falta em vez de supor.
            - Nunca invente pontos, padrões, referências ou estudos. Se não souber,
              diga que não sabe.
        """.trimIndent()
    }
}
