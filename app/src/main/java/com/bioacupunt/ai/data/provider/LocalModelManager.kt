package com.bioacupunt.ai.data.provider

import android.app.ActivityManager
import android.content.Context
import com.bioacupunt.ai.local.LocalModel
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
import java.util.concurrent.TimeUnit

/**
 * Baixa e gerencia os arquivos de modelo no aparelho — dirigido pelo
 * [LocalModelCatalog], nunca por uma URL solta.
 *
 * ## Integridade não é opcional aqui
 *
 * Uma versão anterior desta classe baixava o modelo e só conferia um tamanho
 * mínimo — [ModelIntegrity] existia, mas ninguém o chamava. Isso é exatamente o
 * fail-open que a R3 proíbe: um `.task` é *executado* por runtime nativo C++.
 * Hoje o contrato é: **nenhum byte baixado vira modelo utilizável sem passar por
 * [ModelIntegrity.verify] contra o hash fixado no código.** A verificação acontece
 * no arquivo temporário, ANTES do rename atômico; um hash errado apaga o download,
 * nunca o promove.
 *
 * ## Onde os arquivos moram
 *
 * `filesDir/models/` — storage privado do app, criptografado em repouso nos
 * Androids modernos. Modelo clínico não tem o que fazer em storage compartilhado.
 *
 * ## De onde vêm os bytes
 *
 * Direto do Hugging Face ([LocalModel.downloadUrl]). Sem backend próprio a clínica
 * não redistribui pesos (questão de licença), e a fonte não precisa ser confiada:
 * o hash pinado decide, não o servidor.
 */
class LocalModelManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        // Um modelo tem gigabytes: timeout de corpo curto mataria todo download
        // em rede de clínica. Conexão/handshake continuam com timeout normal.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {

    sealed interface State {
        data object Idle : State
        data class Downloading(val modelId: String, val progress: Float) : State
        /** Hash de GBs leva segundos — a UI precisa dizer "verificando", não congelar. */
        data class Verifying(val modelId: String) : State
        data class Ready(val modelId: String) : State
        data class Failed(val modelId: String, val message: String) : State
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: Flow<State> = _state.asStateFlow()

    private fun modelsDir(): File = File(context.filesDir, MODELS_DIR).apply { mkdirs() }

    fun fileFor(model: LocalModel): File = File(modelsDir(), model.fileName)

    private fun sidecarFor(model: LocalModel): File = File(modelsDir(), model.fileName + ".verified")

    /**
     * Instalado = arquivo presente, com o tamanho exato do catálogo, e com o sidecar
     * `.verified` gravado por um [ModelIntegrity.verify] que passou no download.
     *
     * O sidecar evita re-hashear gigabytes a cada launch. Ele só é gravado após uma
     * verificação completa, e o conteúdo precisa bater com o hash *pinado* — trocar
     * o catálogo de versão invalida instalações antigas automaticamente.
     */
    fun isInstalled(model: LocalModel): Boolean {
        if (!model.isVerifiable) return false
        val file = fileFor(model)
        if (!file.exists() || file.length() != model.sizeBytes) return false
        val recorded = runCatching { sidecarFor(model).readText().trim() }.getOrNull()
        return recorded.equals(model.sha256, ignoreCase = true)
    }

    fun installedModels(): List<LocalModel> = LocalModelCatalog.verifiable.filter { isInstalled(it) }

    /**
     * O modelo que o provider deve usar agora: o escolhido pela médica, se instalado;
     * senão o melhor instalado que este aparelho roda. Null = IA local indisponível,
     * e o app diz isso com todas as letras — nunca finge.
     */
    fun activeModel(): LocalModel? {
        val chosen = prefs.getString(KEY_SELECTED, null)
            ?.let { id -> LocalModelCatalog.byId(id) }
            ?.takeIf { isInstalled(it) }
        if (chosen != null) return chosen
        return LocalModelCatalog.runnableOn(deviceTotalRamMb()).firstOrNull { isInstalled(it) }
    }

    fun selectModel(model: LocalModel) {
        prefs.edit().putString(KEY_SELECTED, model.id).apply()
    }

    fun deviceTotalRamMb(): Int = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        (info.totalMem / (1024 * 1024)).toInt()
    }.getOrDefault(0)

    /** Modelos que ESTE aparelho pode baixar e rodar, melhor primeiro. */
    fun downloadableModels(): List<LocalModel> = LocalModelCatalog.runnableOn(deviceTotalRamMb())

    /**
     * Baixa para um `.part`, verifica integridade no `.part`, e só então renomeia.
     *
     * A ordem importa: o rename atômico garante que o nome real só existe com bytes
     * completos, e a verificação ANTES do rename garante que ele só existe com bytes
     * *provados*. Download interrompido, página de erro salva como modelo, mirror
     * adulterado — tudo morre no `.part` e é apagado.
     */
    suspend fun download(model: LocalModel): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(model.isVerifiable) {
                // R3: sem hash pinado não há o que verificar — então não há download.
                "Modelo ${model.id} não tem SHA-256 fixado; não será baixado."
            }

            val target = fileFor(model)
            if (isInstalled(model)) {
                _state.value = State.Ready(model.id)
                return@runCatching target
            }

            val temp = File(modelsDir(), model.fileName + ".part")
            temp.delete()
            sidecarFor(model).delete()

            _state.value = State.Downloading(model.id, 0f)

            val response = client.newCall(Request.Builder().url(model.downloadUrl()).build()).execute()
            response.use { res ->
                check(res.isSuccessful) { "Falha ao baixar o modelo: HTTP ${res.code}" }
                val body = res.body ?: error("Resposta vazia ao baixar o modelo")
                val total = body.contentLength().takeIf { it > 0 } ?: model.sizeBytes

                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                _state.value = State.Downloading(model.id, downloaded.toFloat() / total)
                            }
                        }
                    }
                }
            }

            _state.value = State.Verifying(model.id)
            when (val verdict = ModelIntegrity.verify(temp, model)) {
                is ModelIntegrity.Result.Valid -> Unit
                is ModelIntegrity.Result.SizeMismatch -> {
                    temp.delete()
                    error("Download incompleto ou arquivo errado (${verdict.actual} bytes; esperado ${verdict.expected}).")
                }
                is ModelIntegrity.Result.HashMismatch -> {
                    temp.delete()
                    error("Integridade falhou: o arquivo baixado não corresponde ao hash fixado. Download descartado.")
                }
                is ModelIntegrity.Result.Missing -> error("Arquivo temporário sumiu durante a verificação.")
                is ModelIntegrity.Result.NotPinned -> {
                    temp.delete()
                    error("Modelo sem hash fixado não pode ser instalado.")
                }
            }

            target.delete()
            check(temp.renameTo(target)) { "Não foi possível finalizar o modelo baixado" }
            // Gravado por último: o sidecar só existe se TUDO acima aconteceu.
            sidecarFor(model).writeText(model.sha256)

            _state.value = State.Ready(model.id)
            target
        }.onFailure { error ->
            _state.value = State.Failed(model.id, error.message ?: "Erro desconhecido")
        }
    }

    /** Frees the storage. The doctor should be able to reclaim GBs without reinstalling. */
    suspend fun delete(model: LocalModel): Boolean = withContext(Dispatchers.IO) {
        sidecarFor(model).delete()
        File(modelsDir(), model.fileName + ".part").delete()
        val deleted = fileFor(model).delete()
        if (prefs.getString(KEY_SELECTED, null) == model.id) {
            prefs.edit().remove(KEY_SELECTED).apply()
        }
        _state.value = State.Idle
        deleted
    }

    companion object {
        private const val MODELS_DIR = "models"
        private const val PREFS = "local_ai"
        private const val KEY_SELECTED = "selected_model_id"
    }
}
