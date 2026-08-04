package com.bioacupunt.ai.data.provider

import android.content.Context
import com.bioacupunt.ai.local.LocalModelCatalog
import com.bioacupunt.ai.local.ModelIntegrity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads and manages the on-device model file.
 *
 * The model cannot ship inside the APK (hundreds of MB to GB, past Play limits), so
 * it is fetched once into app-private storage — `filesDir`, which is sandboxed to
 * this app and, on modern Android, encrypted at rest with the device credentials.
 * A clinical model file has no business sitting in shared external storage.
 */
class LocalModelManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {

    sealed interface State {
        data object Absent : State
        data class Downloading(val progress: Float) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    /**
     * Length of the last file that passed SHA-256 verification. Verifying a multi-GB file
     * streams every byte, and [isModelReady] is polled on every routing decision, so we
     * hash once per distinct file and remember it. The length is a cheap proxy for "same
     * file": a different download has a different size, and any tampering that also
     * preserved the exact byte count would still fail the hash on the next cold call.
     *
     * Declared before [_state] because [_state]'s initializer calls [isModelReady], which
     * reads this field.
     */
    @Volatile
    private var verifiedLength: Long = -1L

    private val _state = MutableStateFlow<State>(
        if (isModelReady()) State.Ready else State.Absent,
    )
    val state: Flow<State> = _state.asStateFlow()

    fun modelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    /**
     * The single question the rest of the app asks before handing this file to the native
     * inference runtime — and it now enforces R3, which it previously did not.
     *
     * Before, this checked only `exists() && length() > 50MB`. The bytes were never matched
     * against a pinned SHA-256, so [com.bioacupunt.ai.local.ModelIntegrity] and
     * [LocalModelCatalog] were dead code for the LLM path: a substituted or corrupt `.task`
     * would have been executed by native C++ regardless. Now the file must also match the
     * hash pinned in [LocalModelCatalog].
     *
     * It **fails closed**: while the catalog hash is empty (`isVerifiable == false`),
     * [ModelIntegrity.verify] returns `NotPinned` *without hashing* — cheap, and it means
     * no model is offered until the operator runs `scripts/pin_models.sh`. That is
     * intentional: an unverified blob is not "maybe okay", it is not offered at all.
     */
    fun isModelReady(): Boolean {
        val file = modelFile()
        if (!file.exists() || file.length() <= MIN_VALID_BYTES) return false

        val model = LocalModelCatalog.byId(MODEL_ID) ?: return false

        // Fast path: this exact file already passed the hash on a previous call.
        if (verifiedLength == file.length()) return true

        val trusted = ModelIntegrity.isTrusted(file, model)
        verifiedLength = if (trusted) file.length() else -1L
        return trusted
    }

    /**
     * Downloads to a temp file and only then renames into place. **Resumable**: a partial
     * `.part` from an earlier attempt is continued via an HTTP `Range` request, never
     * discarded.
     *
     * Resuming is not a nicety here, it is what makes the download possible at all. The
     * file is ~3.9GB over a mobile connection; it will be interrupted. Before this, the
     * method opened with `temp.delete()`, so every retry restarted from zero — combined
     * with the system's 10-minute ceiling on background work (see [ModelDownloadWorker],
     * which now runs in the foreground precisely because of it), the download could never
     * finish no matter how many times it was retried. Observed on a real device: it died
     * at ~100MB after ~12 minutes, and the next attempt would have thrown those bytes away.
     *
     * The atomic rename at the end still matters for a different reason: a truncated file
     * that merely *exists* would make [isModelReady] say yes and hand a corrupt model to
     * the native runtime. The real filename only ever appears once the bytes behind it are
     * complete **and** verified against the pinned SHA-256 (R3).
     */
    suspend fun download(url: String = DEFAULT_MODEL_URL): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                // No invented URL. DEFAULT_MODEL_URL is intentionally empty until the
                // operator hosts the weights and sets the real URL (SecurePreferences
                // .localModelUrl, surfaced in Ajustes > IA). A blank URL fails honestly
                // instead of hitting a dead 404.
                require(url.isNotBlank()) {
                    "URL do modelo não configurada. Defina a URL do modelo em Ajustes > IA."
                }

                val target = modelFile()
                if (isModelReady()) {
                    _state.value = State.Ready
                    return@runCatching target
                }

                val temp = File(context.filesDir, "$MODEL_FILE_NAME.part")
                val alreadyHave = if (temp.exists()) temp.length() else 0L

                // Estado inicial já refletindo o que existe em disco: reabrir o app no meio
                // de um download mostra "62%", não "0%" — a médica não pode achar que perdeu
                // tudo e mandar recomeçar um download de 3,9GB à toa.
                val expectedTotal = LocalModelCatalog.byId(MODEL_ID)?.sizeBytes?.takeIf { it > 0 }
                _state.value = State.Downloading(
                    if (expectedTotal != null) (alreadyHave.toFloat() / expectedTotal) else 0f,
                )

                val request = Request.Builder().url(url).apply {
                    // Continua de onde parou. Se o servidor ignorar (responde 200 em vez
                    // de 206), o bloco abaixo detecta e recomeça do zero — nunca concatena
                    // um arquivo inteiro no fim de um pedaço, que produziria um blob
                    // corrompido que só seria pego lá na frente pelo SHA-256.
                    if (alreadyHave > 0) header("Range", "bytes=$alreadyHave-")
                }.build()

                val response = client.newCall(request).execute()
                response.use { res ->
                    check(res.isSuccessful) { "Falha ao baixar o modelo: HTTP ${res.code}" }
                    val body = res.body ?: error("Resposta vazia ao baixar o modelo")

                    val resuming = res.code == HTTP_PARTIAL_CONTENT && alreadyHave > 0
                    val startFrom = if (resuming) alreadyHave else 0L
                    if (!resuming && alreadyHave > 0) temp.delete()

                    // contentLength() é o que FALTA baixar; o total é isso mais o que já temos.
                    val total = body.contentLength().takeIf { it > 0 }?.plus(startFrom)

                    body.byteStream().use { input ->
                        java.io.FileOutputStream(temp, resuming).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = startFrom
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (total != null) {
                                    _state.value = State.Downloading(downloaded.toFloat() / total)
                                }
                            }
                        }
                    }
                }

                check(temp.length() > MIN_VALID_BYTES) {
                    "Arquivo baixado é pequeno demais para ser um modelo válido"
                }

                // R3 gate on the download path: the bytes are handed to a native C++
                // runtime, so they are verified against the pinned SHA-256 *before* the
                // real filename ever appears. While the catalog hash is empty this fails
                // closed (NotPinned) — the model is refused, not "warned about", because a
                // warning the doctor taps past is not a security control.
                val model = LocalModelCatalog.byId(MODEL_ID)
                    ?: error("Modelo desconhecido no catálogo: $MODEL_ID")
                when (val verdict = ModelIntegrity.verify(temp, model)) {
                    is ModelIntegrity.Result.Valid -> Unit
                    is ModelIntegrity.Result.NotPinned -> {
                        temp.delete()
                        error(
                            "Modelo não verificável: nenhum SHA-256 fixado para ${model.id}. " +
                                "Rode scripts/pin_models.sh e fixe o hash antes de usar o modelo local.",
                        )
                    }
                    is ModelIntegrity.Result.HashMismatch -> {
                        temp.delete()
                        error("Integridade do modelo falhou: o SHA-256 do arquivo baixado não confere.")
                    }
                    is ModelIntegrity.Result.SizeMismatch -> {
                        temp.delete()
                        error("Tamanho do modelo baixado (${verdict.actual}) difere do esperado (${verdict.expected}).")
                    }
                    is ModelIntegrity.Result.Missing -> {
                        temp.delete()
                        error("Arquivo do modelo desapareceu antes da verificação.")
                    }
                }

                check(temp.renameTo(target)) { "Não foi possível finalizar o modelo baixado" }
                verifiedLength = target.length()

                _state.value = State.Ready
                target
            }.onFailure { error ->
                _state.value = State.Failed(error.message ?: "Erro desconhecido")
            }
        }

    /** Frees the storage. The doctor should be able to reclaim GBs without reinstalling. */
    suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        val deleted = modelFile().delete()
        verifiedLength = -1L
        _state.value = State.Absent
        deleted
    }

    companion object {
        const val MODEL_FILE_NAME = "phi-4-mini-instruct-q8.task"

        /**
         * Catalog id whose pinned SHA-256 / size govern this file. Ties the download and
         * readiness checks to [LocalModelCatalog.byId], so R3 is enforced on the LLM path.
         *
         * Trocado de Qwen2.5 para Phi-4 Mini Instruct (2026-07-29). O pedido original do
         * usuário era Llama 3.2 3B Instruct; investigado e rejeitado (ver a entrada
         * `llama-3.2-3b-rejected` em [LocalModelCatalog] e o handoff em CLAUDE.md) por
         * três problemas técnicos reais — só `.litertlm` sem runtime neste app, build
         * GPU-específica, e provável modelo base (não-instruct) — independentes da
         * licença gated que já tinha causado bastante fricção. Phi-4 Mini Instruct tem
         * `.task` real (mesmo runtime já testado do Qwen), MIT (`gated: false`,
         * confirmado via API autenticada — zero fricção de licença), e contexto real de
         * 4096 tokens (3x o do Qwen). sizeBytes/sha256 em [LocalModelCatalog] vêm de
         * `sha256sum` sobre o arquivo real baixado, nunca inventados (R3).
         */
        const val MODEL_ID = "phi-4-mini-instruct"

        /**
         * URL padrão REAL e verificada — a médica não precisa configurar nada.
         *
         * Confirmado com `curl -I` sem nenhum header de autenticação: o Hugging Face
         * responde 200 com redirect público (`user_id=public` na URL assinada do CDN) —
         * mesma facilidade que o Qwen tinha, porque este repo também é `gated: false`
         * (MIT). `X-Linked-Size` bateu exatamente com o `sizeBytes` pinado no catálogo.
         *
         * Continua sendo um hot-link para terceiro: se um dia o Hugging Face sair do ar ou
         * mover o arquivo, o download falha com mensagem clara e a nuvem assume — o app
         * degrada, não quebra.
         *
         * O arquivo baixado daqui é verificado contra o SHA-256 fixado em
         * [LocalModelCatalog] antes de ser aceito (R3) — um link que devolva outro
         * conteúdo é recusado, não executado.
         */
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/" +
                "Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.task?download=true"

        /** Anything under this is a truncated download or an error page, not a model. */
        private const val MIN_VALID_BYTES = 50L * 1024 * 1024

        /** HTTP 206 — o servidor honrou o `Range` e está mandando só o pedaço que falta. */
        private const val HTTP_PARTIAL_CONTENT = 206

        /**
         * Decide qual URL usar de verdade: a personalizada só vale se apontar para um
         * arquivo `.task`; caso contrário cai para [DEFAULT_MODEL_URL].
         *
         * Isto não é paranoia teórica — aconteceu no device em 2026-07-29: foi colada em
         * Ajustes a URL da *página* de um repositório do Hugging Face (um GGUF, formato que
         * este app nem executa). O download trouxe HTML, o R3 recusou corretamente, e a IA
         * ficou parecendo quebrada quando na verdade estava se defendendo. A médica não
         * deveria precisar saber a diferença entre a URL de uma página e a de um arquivo —
         * o padrão embutido funciona sozinho, e é para ele que qualquer coisa estranha volta.
         *
         * Resolvido no momento do download (não só na hora de enfileirar) de propósito: um
         * job já enfileirado com URL ruim também se corrige na próxima execução, em vez de
         * gastar a franquia de dados dela até estourar o limite de tentativas.
         */
        fun resolveUrl(configured: String): String {
            val trimmed = configured.trim()
            val looksLikeModelFile = trimmed.startsWith("http") &&
                (trimmed.substringBefore('?').endsWith(".task"))
            return if (looksLikeModelFile) trimmed else DEFAULT_MODEL_URL
        }
    }
}
