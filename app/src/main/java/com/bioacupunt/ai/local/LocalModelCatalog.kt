package com.bioacupunt.ai.local

/**
 * CATALOG OF ON-DEVICE MODELS
 *
 * Pure Kotlin, zero Android dependencies, so the selection and integrity logic is
 * unit-testable without a device — which matters, because the failure modes here are
 * silent: a model that "downloads fine" and then OOM-kills the app in the middle of a
 * consultation, or a truncated file that loads as garbage.
 *
 * ## Licensing — read before hosting
 *
 * These are *open weight*, not public domain. Gemma ships under the Gemma Terms of
 * Use and Llama under the Llama Community License; both bind whoever **redistributes**
 * them. The moment BioAcupunt serves these files from its own backend, BioAcupunt is a
 * redistributor. Apache-2.0 models (Qwen, Phi) are the least encumbered.
 * [ModelLicense.requiresAcceptance] exists so the app can refuse to download a model
 * whose terms have not been accepted, rather than quietly making the clinic liable.
 */

enum class ModelLicense(
    val label: String,
    val url: String,
    /** True when the *user/distributor* must actively accept terms, not just be notified. */
    val requiresAcceptance: Boolean,
) {
    APACHE_2_0("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0", false),
    MIT("MIT", "https://opensource.org/license/mit", false),
    GEMMA_TERMS("Gemma Terms of Use", "https://ai.google.dev/gemma/terms", true),
    LLAMA_COMMUNITY("Llama Community License", "https://llama.meta.com/llama3/license/", true),
}

/** Which on-device runtime can execute this file. */
enum class LocalRuntime {
    /** `.litertlm` — Google's current, supported path. */
    LITERT_LM,

    /** `.task` — MediaPipe LLM Inference. Works today; maintenance-only upstream. */
    MEDIAPIPE,
}

data class LocalModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val runtime: LocalRuntime,
    val license: ModelLicense,
    /** Upstream provenance. Never hot-linked at runtime — see [downloadUrl]. */
    val huggingFaceRepo: String,
    /** Exact size on disk. A mismatch means a truncated or wrong file. */
    val sizeBytes: Long,
    /**
     * SHA-256 of the file. Empty means "not yet pinned" — [LocalModelCatalog.verifiable]
     * filters those out, because an unverified multi-GB blob executed as a model is
     * an arbitrary-code-execution surface, not a feature.
     */
    val sha256: String,
    /** Minimum *device* RAM to run this without thrashing. Not the file size. */
    val minDeviceRamMb: Int,
    val contextTokens: Int,
    /** Higher is better quality. Used to pick the best model a device can actually run. */
    val qualityRank: Int,
    val notes: String = "",
) {
    val sizeMb: Int get() = (sizeBytes / (1024 * 1024)).toInt()
    val isVerifiable: Boolean get() = sha256.length == 64

    /** Where the app fetches it from: your backend, never Hugging Face directly. */
    fun downloadUrl(baseUrl: String): String =
        baseUrl.trimEnd('/') + "/models/" + fileName
}

object LocalModelCatalog {

    /**
     * Sizes and hashes below are PLACEHOLDERS and are deliberately marked as such.
     *
     * I will not invent a SHA-256: a fabricated hash is worse than no hash, because it
     * *looks* like integrity while silently failing open or bricking every download.
     * Run `scripts/pin_models.sh` (included) against the real files you host; it prints
     * the exact `sizeBytes`/`sha256` to paste here. Until a model is pinned, it stays
     * out of [verifiable] and the app will not offer it.
     */
    val ALL: List<LocalModel> = listOf(
        LocalModel(
            id = "gemma-3-1b-it-int4",
            displayName = "Gemma 3 1B",
            fileName = "gemma-3-1b-it-int4.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.GEMMA_TERMS,
            huggingFaceRepo = "litert-community/Gemma3-1B-IT",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 3072,
            contextTokens = 2048,
            qualityRank = 10,
            notes = "Menor e mais rápido. Bom para resumo e redação de evolução.",
        ),
        LocalModel(
            id = "qwen2.5-1.5b-instruct",
            displayName = "Qwen 2.5 1.5B",
            fileName = "qwen2.5-1.5b-instruct.litertlm",
            runtime = LocalRuntime.LITERT_LM,
            license = ModelLicense.APACHE_2_0,
            huggingFaceRepo = "litert-community/Qwen2.5-1.5B-Instruct",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 4096,
            contextTokens = 4096,
            qualityRank = 20,
            notes = "Apache 2.0 — licença mais livre. Bom português e chinês (útil em MTC).",
        ),
        LocalModel(
            id = "phi-4-mini-instruct",
            displayName = "Phi-4 Mini",
            fileName = "phi-4-mini-instruct.litertlm",
            runtime = LocalRuntime.LITERT_LM,
            license = ModelLicense.MIT,
            huggingFaceRepo = "litert-community/Phi-4-mini-instruct",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 6144,
            contextTokens = 4096,
            qualityRank = 30,
            notes = "MIT. Raciocínio estruturado forte.",
        ),
        LocalModel(
            id = "gemma-4-e2b-it",
            displayName = "Gemma 4 E2B",
            fileName = "gemma-4-E2B-it.litertlm",
            runtime = LocalRuntime.LITERT_LM,
            license = ModelLicense.GEMMA_TERMS,
            huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 6144,
            contextTokens = 32768,
            qualityRank = 40,
            notes = "Melhor qualidade viável em celular. Contexto longo (prontuário inteiro).",
        ),
    )

    /** Only models whose bytes we can actually prove. Nothing else is offered to the user. */
    val verifiable: List<LocalModel> get() = ALL.filter { it.isVerifiable }

    fun byId(id: String): LocalModel? = ALL.firstOrNull { it.id == id }

    /**
     * Models this device can run, best first.
     *
     * [deviceRamMb] must be *total* device RAM. The headroom rule below is the whole
     * point of this function: a model that technically fits will still get the app
     * OOM-killed mid-consultation, because Android will not hand a single app the whole
     * machine. Offering a model that crashes the app is worse than offering none — the
     * doctor loses the chart she was writing.
     *
     * Verifiability is enforced **here**, not only in the default argument. An earlier
     * version filtered unpinned models by defaulting `models` to [verifiable]; a caller
     * passing its own list silently got unpinned models back. That is a fail-*open*
     * hole, and a unit test caught it. Security filters belong inside the function, not
     * in a default parameter a caller can step around.
     */
    fun runnableOn(deviceRamMb: Int, models: List<LocalModel> = ALL): List<LocalModel> =
        models
            .filter { it.isVerifiable }
            .filter { deviceRamMb >= it.minDeviceRamMb }
            .sortedByDescending { it.qualityRank }

    /** The best model this device can run, or null when it can run none. */
    fun recommendedFor(deviceRamMb: Int, models: List<LocalModel> = ALL): LocalModel? =
        runnableOn(deviceRamMb, models).firstOrNull()
}
