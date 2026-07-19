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

    /**
     * Download direto do Hugging Face, sem backend próprio.
     *
     * Isso é uma decisão, não um acidente: (1) sem backend, a clínica não vira
     * REDISTRIBUIDORA dos pesos — quem baixa do HF aceita os termos com o publicador,
     * não conosco; (2) a integridade não depende da fonte: o SHA-256 fixado no código
     * é verificado sobre os bytes baixados ANTES de o arquivo virar utilizável
     * (ver LocalModelManager), então um mirror comprometido ou um MITM produz um
     * download rejeitado, nunca um modelo carregado.
     */
    fun downloadUrl(): String =
        "https://huggingface.co/$huggingFaceRepo/resolve/main/$fileName?download=true"
}

object LocalModelCatalog {

    /**
     * O runtime que o [com.bioacupunt.ai.data.provider.LocalLlmProvider] consegue
     * executar HOJE. Um modelo pinado cujo runtime não está aqui continua fora de
     * [runnableOn]: hash correto não adianta se o engine não abre o formato — seria
     * um crash com cara de feature. Quando a migração para LiteRT-LM acontecer,
     * adicione [LocalRuntime.LITERT_LM] aqui e o Gemma 4 E2B (já pinado abaixo)
     * passa a ser oferecido sem mais nenhuma mudança.
     */
    val SUPPORTED_RUNTIMES: Set<LocalRuntime> = setOf(LocalRuntime.MEDIAPIPE)

    /**
     * PROVENIÊNCIA DOS HASHES — leia antes de tocar.
     *
     * Os `sha256`/`sizeBytes` abaixo NÃO foram inventados (R3): são o registro LFS
     * oficial do Hugging Face para cada arquivo — o oid LFS *é* o SHA-256 do blob —
     * lido de `https://huggingface.co/api/models/<repo>/tree/main?blobs=true`
     * em 2026-07-16. É o mesmo valor que `scripts/pin_models.sh` imprime após baixar
     * e conferir localmente; o script continua sendo o caminho para re-pinar.
     *
     * Os modelos Gemma 3 1B exigem aceite de licença no HF e o registro LFS não é
     * público — por isso a entrada permanece SEM hash e, portanto, fora de
     * [verifiable]. Fail-closed, como manda a R3: sem prova, sem oferta.
     */
    val ALL: List<LocalModel> = listOf(
        LocalModel(
            id = "qwen2.5-1.5b-instruct-q8",
            displayName = "Qwen 2.5 1.5B",
            fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.APACHE_2_0,
            huggingFaceRepo = "litert-community/Qwen2.5-1.5B-Instruct",
            sizeBytes = 1_598_556_720L,
            sha256 = "82968d0a6c3872cf016fdbcfc591571605f4c7fd2b0f64d2533df502cc6596b3",
            minDeviceRamMb = 4096,
            contextTokens = 4096,
            qualityRank = 20,
            notes = "Apache 2.0 — licença mais livre, sem aceite. Bom português e chinês (útil em MTC). ~1,5 GB.",
        ),
        LocalModel(
            id = "phi-4-mini-instruct-q8",
            displayName = "Phi-4 Mini",
            fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.MIT,
            huggingFaceRepo = "litert-community/Phi-4-mini-instruct",
            sizeBytes = 3_910_050_199L,
            sha256 = "88665a75f6a0b5083ce65255139212ff6da705d5f682edbbd109eae784b2173c",
            minDeviceRamMb = 8192,
            contextTokens = 4096,
            qualityRank = 30,
            notes = "MIT. Raciocínio estruturado forte. ~3,6 GB — só para aparelhos com folga.",
        ),
        LocalModel(
            id = "gemma-3-1b-it-int4",
            displayName = "Gemma 3 1B",
            fileName = "gemma3-1b-it-int4.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.GEMMA_TERMS,
            huggingFaceRepo = "litert-community/Gemma3-1B-IT",
            // Sem hash de propósito: o repositório é gated (exige aceite dos Gemma
            // Terms) e o registro LFS não é legível sem ele. Pinar exige rodar
            // scripts/pin_models.sh com HF_TOKEN de uma conta que aceitou os termos.
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 3072,
            contextTokens = 2048,
            qualityRank = 10,
            notes = "Menor e mais rápido, mas exige aceite dos Gemma Terms para pinar.",
        ),
        LocalModel(
            id = "gemma-4-e2b-it",
            displayName = "Gemma 4 E2B",
            fileName = "gemma-4-E2B-it.litertlm",
            runtime = LocalRuntime.LITERT_LM,
            license = ModelLicense.GEMMA_TERMS,
            huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
            sizeBytes = 2_588_147_712L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            minDeviceRamMb = 6144,
            contextTokens = 32768,
            qualityRank = 40,
            notes = "Melhor qualidade viável em celular; aguarda a migração do runtime para LiteRT-LM.",
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
            .filter { it.runtime in SUPPORTED_RUNTIMES }
            .filter { deviceRamMb >= it.minDeviceRamMb }
            .sortedByDescending { it.qualityRank }

    /** The best model this device can run, or null when it can run none. */
    fun recommendedFor(deviceRamMb: Int, models: List<LocalModel> = ALL): LocalModel? =
        runnableOn(deviceRamMb, models).firstOrNull()
}
