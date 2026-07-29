package com.bioacupunt.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalModelCatalogTest {

    private fun model(
        id: String,
        ram: Int,
        quality: Int,
        sha: String = "a".repeat(64),
        size: Long = 1024,
    ) = LocalModel(
        id = id,
        displayName = id,
        fileName = "$id.litertlm",
        runtime = LocalRuntime.LITERT_LM,
        license = ModelLicense.APACHE_2_0,
        huggingFaceRepo = "litert-community/$id",
        sizeBytes = size,
        sha256 = sha,
        minDeviceRamMb = ram,
        contextTokens = 4096,
        qualityRank = quality,
    )

    private val small = model("small", ram = 3072, quality = 10)
    private val mid = model("mid", ram = 4096, quality = 20)
    private val big = model("big", ram = 8192, quality = 40)
    private val all = listOf(small, mid, big)

    // -- Device gating ------------------------------------------------------

    @Test
    fun recommendsBestModelDeviceCanActuallyRun() {
        assertEquals(mid, LocalModelCatalog.recommendedFor(4096, all))
        assertEquals(big, LocalModelCatalog.recommendedFor(8192, all))
    }

    @Test
    fun neverRecommendsAModelThatWouldOomTheDevice() {
        // 3 GB phone must not be handed the 8 GB model, even though it is "best".
        val chosen = LocalModelCatalog.recommendedFor(3072, all)
        assertEquals(small, chosen)
        assertTrue(chosen!!.minDeviceRamMb <= 3072)
    }

    @Test
    fun weakDeviceGetsNothingRatherThanACrash() {
        assertNull(
            "Melhor não oferecer modelo do que travar o app da doutora",
            LocalModelCatalog.recommendedFor(2048, all),
        )
        assertTrue(LocalModelCatalog.runnableOn(2048, all).isEmpty())
    }

    @Test
    fun runnableListIsOrderedByQualityDescending() {
        val runnable = LocalModelCatalog.runnableOn(16384, all)
        assertEquals(listOf(big, mid, small), runnable)
    }

    // -- Fail-closed on unpinned models -------------------------------------

    @Test
    fun unpinnedModelsAreNotOffered() {
        val unpinned = model("unpinned", ram = 1024, quality = 99, sha = "")
        assertFalse(unpinned.isVerifiable)

        val offered = LocalModelCatalog.runnableOn(16384, listOf(unpinned) + all)
        assertFalse(
            "Modelo sem hash fixado não pode ser oferecido, mesmo sendo o 'melhor'",
            offered.contains(unpinned),
        )
    }

    @Test
    fun shippedCatalogPinnedEntriesAreWellFormed() {
        // Substitui um teste que afirmava "nada está pinado, logo nada é oferecido".
        // Isso deixou de ser verdade quando o Qwen2.5 recebeu um SHA-256 REAL — obtido
        // com sha256sum sobre o arquivo baixado e conferido de novo no próprio aparelho,
        // nunca inventado (R3). O invariante que importa nunca foi "nada pinado"; é
        // "o que está pinado está pinado direito".
        val pinned = LocalModelCatalog.verifiable
        assertTrue("Deve haver ao menos um modelo local pinado", pinned.isNotEmpty())
        pinned.forEach { m ->
            assertEquals("SHA-256 precisa ter 64 caracteres", 64, m.sha256.length)
            assertTrue(
                "SHA-256 só pode conter hexadecimais minúsculos",
                m.sha256.all { c -> c.isDigit() || c in 'a'..'f' },
            )
            assertTrue("Tamanho fixado não pode ser zero", m.sizeBytes > 0)
        }
    }

    @Test
    fun modelInUseIsPinned_butGatedModelsStayUnofferable() {
        assertTrue(
            "O modelo local em uso precisa estar pinado, senão nada é oferecido",
            LocalModelCatalog.byId("qwen2.5-1.5b-instruct")?.isVerifiable == true,
        )
        // O Gemma continua no catálogo, e continua SEM hash de propósito: o repo dele é
        // gated (o Hugging Face responde HTTP 401 sem conta com a licença aceita), então
        // não há como baixá-lo nem verificá-lo. Fail-closed — ele nunca é oferecido.
        assertFalse(
            "Gemma segue não-oferecível: repo gated, sem hash fixado",
            LocalModelCatalog.verifiable.any { it.id == "gemma-3-1b-it-int4" },
        )
        // Llama 3.2 3B (2026-07-29): investigado como candidato e REJEITADO — só existe
        // em .litertlm (sem runtime no app), build GPU-específica, provável modelo base
        // (não-instruct). Fica no catálogo só como registro histórico e nunca será
        // pinado. Ver comentário completo em LocalModelCatalog.kt.
        assertFalse(
            "Llama fica permanentemente não-oferecível — rejeitado, não só pendente",
            LocalModelCatalog.verifiable.any { it.id == "llama-3.2-3b-rejected" },
        )
    }

    @Test
    fun downloadUrlHelperTargetsSelfHosting_notHuggingFace() {
        // Este helper é o caminho de AUTO-HOSPEDAGEM e continua sendo: monta
        // <seu-backend>/models/<arquivo>.
        //
        // Não confundir com `LocalModelManager.DEFAULT_MODEL_URL`, que HOJE aponta para o
        // Hugging Face de propósito — é o que permite a médica baixar o modelo sem colar
        // URL nenhuma. Isso só é aceitável porque o Qwen2.5 é Apache 2.0 e não-gated; com
        // o Gemma seria impossível (401) e legalmente mais delicado. Quem quiser hospedar
        // por conta própria preenche a URL em Ajustes > IA, que tem precedência.
        val url = mid.downloadUrl("https://api.exemplo.com/")
        assertEquals("https://api.exemplo.com/models/mid.litertlm", url)
        assertFalse(url.contains("huggingface"))
    }
}

class ModelIntegrityTest {

    private fun tempFileWith(bytes: ByteArray): File =
        File.createTempFile("model", ".bin").apply {
            writeBytes(bytes)
            deleteOnExit()
        }

    private fun modelFor(file: File, hash: String) = LocalModel(
        id = "test",
        displayName = "test",
        fileName = file.name,
        runtime = LocalRuntime.LITERT_LM,
        license = ModelLicense.APACHE_2_0,
        huggingFaceRepo = "x/y",
        sizeBytes = file.length(),
        sha256 = hash,
        minDeviceRamMb = 1024,
        contextTokens = 1024,
        qualityRank = 1,
    )

    @Test
    fun sha256MatchesKnownVector() {
        // SHA-256("abc") — the canonical NIST test vector. If this drifts, the whole
        // integrity story is broken and every other test here is meaningless.
        val file = tempFileWith("abc".toByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ModelIntegrity.sha256(file),
        )
    }

    @Test
    fun validFileIsTrusted() {
        val file = tempFileWith("modelo".toByteArray())
        val model = modelFor(file, ModelIntegrity.sha256(file))
        assertTrue(ModelIntegrity.isTrusted(file, model))
    }

    @Test
    fun tamperedFileIsRejected() {
        val file = tempFileWith("modelo".toByteArray())
        val model = modelFor(file, ModelIntegrity.sha256(file))

        // Same length, different bytes: size check passes, hash must catch it.
        file.writeBytes("MODELO".toByteArray())

        val result = ModelIntegrity.verify(file, model)
        assertTrue("Arquivo adulterado deve ser rejeitado", result is ModelIntegrity.Result.HashMismatch)
        assertFalse(ModelIntegrity.isTrusted(file, model))
    }

    @Test
    fun truncatedDownloadIsRejectedBySize() {
        val file = tempFileWith("modelo completo".toByteArray())
        val model = modelFor(file, ModelIntegrity.sha256(file))

        file.writeBytes("modelo".toByteArray()) // interrupted download

        val result = ModelIntegrity.verify(file, model)
        assertTrue(result is ModelIntegrity.Result.SizeMismatch)
    }

    @Test
    fun missingFileIsRejected() {
        val file = tempFileWith("x".toByteArray())
        val model = modelFor(file, ModelIntegrity.sha256(file))
        file.delete()

        assertEquals(ModelIntegrity.Result.Missing, ModelIntegrity.verify(file, model))
    }

    @Test
    fun unpinnedModelFailsClosed_neverOpen() {
        val file = tempFileWith("qualquer coisa".toByteArray())
        val unpinned = modelFor(file, "") // no hash

        val result = ModelIntegrity.verify(file, unpinned)
        assertTrue(result is ModelIntegrity.Result.NotPinned)
        assertFalse(
            "Sem hash fixado, o arquivo NUNCA pode ser considerado confiável",
            ModelIntegrity.isTrusted(file, unpinned),
        )
    }
}
