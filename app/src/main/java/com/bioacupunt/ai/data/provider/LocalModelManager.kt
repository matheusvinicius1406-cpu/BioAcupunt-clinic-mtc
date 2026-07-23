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
     * Downloads to a temp file and only then renames into place.
     *
     * This matters more than it looks: a download interrupted at 90% — the user walked
     * out of wifi, the OS killed the app — would otherwise leave a truncated file that
     * *exists*, so [isModelReady] would say yes and the inference engine would crash
     * on a corrupt model. Atomic rename means the real filename only ever appears when
     * the bytes behind it are complete.
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
                temp.delete()

                _state.value = State.Downloading(0f)

                val response = client.newCall(Request.Builder().url(url).build()).execute()
                response.use { res ->
                    check(res.isSuccessful) { "Falha ao baixar o modelo: HTTP ${res.code}" }
                    val body = res.body ?: error("Resposta vazia ao baixar o modelo")
                    val total = body.contentLength().takeIf { it > 0 }

                    body.byteStream().use { input ->
                        temp.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = 0L
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
        const val MODEL_FILE_NAME = "gemma-3-1b-it-int4.task"

        /**
         * Catalog id whose pinned SHA-256 / size govern this file. Ties the download and
         * readiness checks to [LocalModelCatalog.byId], so R3 is enforced on the LLM path.
         */
        const val MODEL_ID = "gemma-3-1b-it-int4"

        /**
         * PENDÊNCIA DO USUÁRIO — intentionally empty, not an invented URL.
         *
         * The old value (`https://bioacupunt-api.onrender.com/models/…`) returns 404: the
         * backend has no such route, so it was dead weight that made every download fail.
         * Host the weights yourself (S3/R2/CDN of your choice) rather than hot-linking a
         * third party — you control availability and keep the Gemma licence-acceptance
         * flow in your own hands — then set the real URL in `SecurePreferences.localModelUrl`
         * (Ajustes > IA). Until then [download] rejects a blank URL with a clear message.
         */
        const val DEFAULT_MODEL_URL = ""

        /** Anything under this is a truncated download or an error page, not a model. */
        private const val MIN_VALID_BYTES = 50L * 1024 * 1024
    }
}
