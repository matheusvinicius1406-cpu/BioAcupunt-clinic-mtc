package com.bioacupunt.ai.data.provider

import android.content.Context
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

    private val _state = MutableStateFlow<State>(
        if (modelFile().exists()) State.Ready else State.Absent,
    )
    val state: Flow<State> = _state.asStateFlow()

    fun modelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isModelReady(): Boolean = modelFile().let { it.exists() && it.length() > MIN_VALID_BYTES }

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
                check(temp.renameTo(target)) { "Não foi possível finalizar o modelo baixado" }

                _state.value = State.Ready
                target
            }.onFailure { error ->
                _state.value = State.Failed(error.message ?: "Erro desconhecido")
            }
        }

    /** Frees the storage. The doctor should be able to reclaim GBs without reinstalling. */
    suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        val deleted = modelFile().delete()
        _state.value = State.Absent
        deleted
    }

    companion object {
        const val MODEL_FILE_NAME = "gemma-3-1b-it-int4.task"

        /**
         * Host the weights yourself (S3/R2/your backend) rather than hot-linking a
         * third party: you control availability, and you keep the licence acceptance
         * flow in your own hands. Gemma is openly available but licence-bound —
         * shipping it means accepting those terms.
         */
        const val DEFAULT_MODEL_URL = "https://bioacupunt-api.onrender.com/models/gemma-3-1b-it-int4.task"

        /** Anything under this is a truncated download or an error page, not a model. */
        private const val MIN_VALID_BYTES = 50L * 1024 * 1024
    }
}
