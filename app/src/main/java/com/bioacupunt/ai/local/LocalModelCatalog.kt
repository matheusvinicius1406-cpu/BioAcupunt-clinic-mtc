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
        // PINADO E EM USO — este é o modelo local padrão do app.
        //
        // Trocado de Gemma para Qwen por um motivo concreto, não estético: o repo
        // `litert-community/Gemma3-1B-IT` responde HTTP 401 `GatedRepo` sem uma conta
        // Hugging Face com a licença Gemma aceita, então nenhum download automático
        // conseguia buscá-lo. Qwen2.5 é `gated: false` (Apache 2.0) e, segundo a nota
        // que já estava aqui, tem português e chinês melhores — os dois idiomas que
        // mais importam num app de MTC.
        //
        // sizeBytes/sha256 abaixo NÃO foram inventados (R3): saíram de `sha256sum` sobre
        // o arquivo real baixado de
        // Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task, e o tamanho bate
        // exatamente com o `x-linked-size` que o Hugging Face reporta.
        LocalModel(
            id = "qwen2.5-1.5b-instruct",
            displayName = "Qwen 2.5 1.5B",
            fileName = "qwen2.5-1.5b-instruct-q8.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.APACHE_2_0,
            huggingFaceRepo = "litert-community/Qwen2.5-1.5B-Instruct",
            sizeBytes = 1_597_913_616L,
            sha256 = "8d867a7c93a6acf2892f08e0174e2f6f351ad256b7e3cfb6d6cd9c89794b42e0",
            minDeviceRamMb = 4096,
            // ekv1280: o KV cache deste build é 1280 tokens. Pedir mais que isso ao
            // runtime não "trunca" — ele falha ao criar a sessão.
            contextTokens = 1280,
            qualityRank = 20,
            notes = "Apache 2.0 — licença mais livre. Bom português e chinês (útil em MTC).",
        ),
        // INVESTIGADO E REJEITADO (2026-07-29) — pedido do usuário era trocar o motor
        // local para Llama 3.2 3B Instruct. `litert-community/Llama-3.2-3B-Instruct`
        // (a URL óbvia, e a que resultados de busca insistiam existir) NÃO EXISTE —
        // confirmado com `huggingface_hub` autenticado listando os ~282 repos reais
        // do org `litert-community`. O repo real é `litert-community/Llama-3.2-3B`
        // (sem "-Instruct"), e ele tem três problemas técnicos reais, não só a
        // licença (Llama Community License, gated="auto" — auto-aprovação, mas ainda
        // exige aceite):
        //   1. Só existe em `.litertlm` (arquivo real:
        //      `llama3_2_3b_mixed_int4_gpu.litertlm`, ~2.06GB). Não há `.task`. Este
        //      app só tem runtime implementado para `.task` (MediaPipe) —
        //      `LocalRuntime.LITERT_LM` não tem código nenhum por trás ainda.
        //   2. É uma build específica de GPU (`_gpu` no nome) — compatibilidade
        //      incerta entre aparelhos Android variados.
        //   3. A tag `base_model` desse arquivo aponta para `meta-llama/Llama-3.2-3B`
        //      (SEM "-Instruct") — sinal forte de que é o modelo base/pré-treinado,
        //      não o afinado para seguir instrução. Não haveria como confirmar sem
        //      baixar o conteúdo gated, e não faz sentido pedir mais um aceite de
        //      licença para um modelo que provavelmente não serve como assistente.
        // Não fica pinado nem será — mantido aqui só como registro, para nenhuma
        // sessão futura repetir a mesma investigação. Ver CLAUDE.md "Onde parei
        // (2026-07-29)" para o histórico completo da tentativa.
        LocalModel(
            id = "llama-3.2-3b-rejected",
            displayName = "Llama 3.2 3B (rejeitado)",
            fileName = "llama3_2_3b_mixed_int4_gpu.litertlm",
            runtime = LocalRuntime.LITERT_LM,
            license = ModelLicense.LLAMA_COMMUNITY,
            huggingFaceRepo = "litert-community/Llama-3.2-3B",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 8192,
            contextTokens = 0,
            qualityRank = 0,
            notes = "REJEITADO: só .litertlm (sem runtime no app), build GPU-específica, provável modelo base (não-instruct). Ver comentário acima.",
        ),
        // PINADO E ATIVO (2026-07-29) — substituiu o Qwen 2.5 como modelo local padrão.
        // Escolhido depois de o Llama 3.2 3B (acima) ser investigado e rejeitado: este
        // é `.task` real (mesmo runtime MediaPipe já testado, não `.litertlm`), MIT
        // (`gated: False`, confirmado via `huggingface_hub` autenticado — sem fricção
        // de licença nenhuma), "instruct" no próprio nome, e contexto real de 4096
        // tokens (`ekv4096`, mais que o triplo do Qwen). sizeBytes/sha256 vêm de
        // `sha256sum` sobre o arquivo real baixado de
        // `litert-community/Phi-4-mini-instruct/resolve/main/
        // Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task` — nunca inventados (R3).
        LocalModel(
            id = "phi-4-mini-instruct",
            displayName = "Phi-4 Mini Instruct",
            fileName = "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task",
            runtime = LocalRuntime.MEDIAPIPE,
            license = ModelLicense.MIT,
            huggingFaceRepo = "litert-community/Phi-4-mini-instruct",
            sizeBytes = 3_910_050_199L,
            sha256 = "88665a75f6a0b5083ce65255139212ff6da705d5f682edbbd109eae784b2173c",
            // ~3.9GB de peso, maior que o Qwen (1.5GB, exige 4096). Estimativa por
            // proporção — ainda não validada em device real.
            minDeviceRamMb = 8192,
            contextTokens = 4096,
            qualityRank = 30,
            notes = "MIT, sem licença restrita. Raciocínio estruturado forte, contexto 3x maior que o Qwen.",
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
