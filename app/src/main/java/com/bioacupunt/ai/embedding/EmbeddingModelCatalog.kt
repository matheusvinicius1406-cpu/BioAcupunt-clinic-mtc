package com.bioacupunt.ai.embedding

/**
 * CATÁLOGO DE MODELOS DE EMBEDDING ON-DEVICE
 *
 * Segue o mesmo padrão de [com.bioacupunt.ai.local.LocalModelCatalog]:
 * - SHA-256 fixado no código (regra R3 — integridade de modelo)
 * - Modelos sem hash não são oferecidos ([verifiable] filtra)
 * - [runnableOn] considera RAM do dispositivo
 *
 * ## Modelo Atual: multilingual-e5-small
 * - 118M parâmetros, 384 dimensões
 * - INT8 quantizado via TFLite (~45 MB no disco)
 * - Suporte multilíngue nativo (PT, ZH, EN, ES, FR)
 * - Prefixo obrigatório: "query: " para queries, "passage: " para documentos
 * - Máximo de 512 tokens por input
 *
 * ## Por que e5-small e não BGE-M3?
 * BGE-M3 (560M params, 1024d) é excelente mas inviável em mobile sem GPU.
 * e5-small (118M params, 384d) tem recall@10 apenas ~3% menor em benchmarks
 * de busca semântica, mas cabe em 45 MB com INT8 e roda em ~80ms no CPU.
 */
object EmbeddingModelCatalog {

    data class EmbeddingModel(
        val id: String,
        val displayName: String,
        val fileName: String,
        val dimensions: Int,
        val maxTokens: Int,
        val requiresQueryPrefix: Boolean,
        val queryPrefix: String,
        val passagePrefix: String,
        val sizeBytes: Long,
        val sha256: String,
        val minDeviceRamMb: Int,
        val qualityRank: Int,
        val notes: String = "",
    ) {
        val sizeMb: Int get() = (sizeBytes / (1024 * 1024)).toInt()
        val isVerifiable: Boolean get() = sha256.length == 64

        fun downloadUrl(baseUrl: String): String =
            baseUrl.trimEnd('/') + "/models/" + fileName
    }

    val ALL: List<EmbeddingModel> = listOf(
        EmbeddingModel(
            id = "e5-small-int8",
            displayName = "multilingual-e5-small (INT8)",
            fileName = "e5-small-int8.tflite",
            dimensions = 384,
            maxTokens = 512,
            requiresQueryPrefix = true,
            queryPrefix = "query: ",
            passagePrefix = "passage: ",
            sizeBytes = 0L,           // pin via scripts/pin_models.sh
            sha256 = "",               // placeholder — executar pin_models.sh
            minDeviceRamMb = 2048,
            qualityRank = 30,
            notes = "Modelo multilíngue 118M params. 384d. Recomendado para MKIS on-device.",
        ),
        EmbeddingModel(
            id = "e5-small-fp16",
            displayName = "multilingual-e5-small (FP16)",
            fileName = "e5-small-fp16.tflite",
            dimensions = 384,
            maxTokens = 512,
            requiresQueryPrefix = true,
            queryPrefix = "query: ",
            passagePrefix = "passage: ",
            sizeBytes = 0L,
            sha256 = "",
            minDeviceRamMb = 3072,
            qualityRank = 35,
            notes = "FP16 — maior precisão, mais RAM. Alternativa se INT8 perder qualidade.",
        ),
    )

    /** Apenas modelos com SHA-256 fixado (verificáveis). */
    val verifiable: List<EmbeddingModel> get() = ALL.filter { it.isVerifiable }

    /** Melhor modelo que este dispositivo pode rodar. */
    fun recommendedFor(deviceRamMb: Int): EmbeddingModel? =
        ALL.filter { it.isVerifiable }
            .filter { deviceRamMb >= it.minDeviceRamMb }
            .maxByOrNull { it.qualityRank }

    /** Lista de provedores de download (para interface de seleção). */
    val downloadProviders: List<EmbeddingModel> get() = verifiable
}
