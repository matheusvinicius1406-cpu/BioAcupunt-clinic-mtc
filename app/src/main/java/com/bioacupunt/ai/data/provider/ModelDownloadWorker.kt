package com.bioacupunt.ai.data.provider

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bioacupunt.di.AppContainer
import com.bioacupunt.observability.AppLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Baixa o modelo local (~3,9GB) como serviço de PRIMEIRO PLANO, sobrevivendo à navegação,
 * ao app indo para segundo plano e ao teto de execução do WorkManager.
 *
 * ## Três bugs distintos, achados em sequência no mesmo aparelho
 *
 * 1. **Coroutine presa à composição.** [LocalModelCard] disparava o download com
 *    `rememberCoroutineScope().launch { manager.download(url) }` — cancelado no instante
 *    em que a médica saía da tela de Ajustes. Sintoma: dois downloads truncados no MESMO
 *    byte count exato (344204). Corrigido movendo para este Worker.
 *
 * 2. **Teto de 10 minutos.** Um `CoroutineWorker` comum é interrompido pelo sistema após
 *    ~10 min. Confirmado no device: o download morreu aos ~100MB depois de ~12 minutos de
 *    execução. Para um arquivo de 3,9GB isso é um teto intransponível, não um contratempo.
 *    Corrigido com [getForegroundInfo]/`setForeground` — trabalho em primeiro plano não
 *    tem esse limite, e de quebra a médica passa a ver o progresso na barra de notificação
 *    em vez de um download invisível que ela não sabe se está acontecendo.
 *
 * 3. **Retry que recomeçava do zero.** `download()` abria com `temp.delete()`, então cada
 *    nova tentativa jogava fora tudo que já tinha vindo. Somado ao item 2, o download era
 *    *matematicamente impossível* de concluir: morria a cada 10 min e recomeçava do zero,
 *    para sempre. Corrigido em [LocalModelManager.download] com `Range` HTTP.
 *
 * Os três juntos explicam por que a IA "não funcionava" — nenhum deles era falha de rede,
 * e nenhum aparecia como erro na tela.
 *
 * `runAttemptCount` limita a repetição: uma queda de wifi merece nova tentativa, mas uma
 * URL quebrada ou um hash adulterado (R3) nunca vira sucesso só de insistir — sem teto, o
 * Worker ficaria retentando para sempre, gastando bateria e dado móvel à toa.
 */
class ModelDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(progressPercent = null)

    override suspend fun doWork(): Result {
        // Resolvido aqui, e não só na hora de enfileirar: uma URL personalizada inválida
        // (ou um job antigo já na fila com URL ruim) volta sozinho para o padrão embutido,
        // em vez de queimar tentativas baixando HTML que o R3 vai recusar de qualquer jeito.
        val url = LocalModelManager.resolveUrl(inputData.getString(KEY_URL).orEmpty())

        // Promove a primeiro plano ANTES de começar, senão o sistema derruba no meio.
        runCatching { setForeground(buildForegroundInfo(progressPercent = null)) }
            .onFailure { AppLogger.w("ModelDownloadWorker", "Não foi possível ir para primeiro plano", it) }

        // Espelha o progresso do manager na notificação. Observa o mesmo StateFlow que a
        // UI observa — uma fonte de verdade só, sem contador paralelo que possa divergir.
        val progressJob = launchProgressMirror()

        return try {
            AppContainer.localModelManager.download(url).fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    AppLogger.w("ModelDownloadWorker", "Falha ao baixar modelo local (tentativa $runAttemptCount)", error)
                    // Retry preserva o .part: a próxima tentativa continua de onde parou.
                    if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                },
            )
        } finally {
            progressJob.cancel()
        }
    }

    private fun launchProgressMirror() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
        AppContainer.localModelManager.state.collectLatest { state ->
            if (state is LocalModelManager.State.Downloading) {
                val pct = (state.progress * 100).toInt().coerceIn(0, 100)
                runCatching { setForeground(buildForegroundInfo(pct)) }
            }
        }
    }

    private fun buildForegroundInfo(progressPercent: Int?): ForegroundInfo {
        ensureChannel()
        val text = progressPercent
            ?.let { "Baixando… $it% · pode usar o app normalmente" }
            ?: "Preparando o download…"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Instalando a IA do BioAcupunt")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent ?: 0, progressPercent == null)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Instalação da IA", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progresso do download do modelo de IA que roda no aparelho."
                setShowBadge(false)
            },
        )
    }

    companion object {
        const val KEY_URL = "url"
        const val UNIQUE_WORK_NAME = "model_download"
        private const val MAX_ATTEMPTS = 5
        private const val CHANNEL_ID = "ai_model_download"
        private const val NOTIFICATION_ID = 4711

        /**
         * Enfileiramento MANUAL (a médica tocou "Baixar agora"): qualquer conexão serve,
         * porque ela pediu explicitamente e sabe que está pedindo.
         *
         * Ponto único usado tanto por [LocalModelCard] (Ajustes) quanto pelo banner de
         * prontidão em Inteligência — nunca dois jeitos diferentes de disparar o mesmo
         * download.
         */
        fun enqueue(context: Context, url: String) {
            enqueue(context, url, NetworkType.CONNECTED)
        }

        /**
         * Enfileiramento AUTOMÁTICO, disparado no start do app quando o modelo ainda não
         * está pronto — para a IA se instalar sozinha, sem a médica precisar descobrir
         * Ajustes > IA nem colar URL nenhuma.
         *
         * `UNMETERED` (só Wi-Fi) porque são ~3,9GB: começar isso sozinho no 4G dela seria
         * gastar a franquia sem ter perguntado. No 4G o banner da Inteligência continua
         * oferecendo o botão manual, que aí sim usa qualquer conexão — a escolha cara
         * existe, só não acontece pelas costas dela.
         *
         * `KEEP` garante que reabrir o app no meio de um download não reinicia do zero.
         * `download()` já sai na hora se o modelo estiver pronto, então enfileirar sempre
         * é barato e não precisa de checagem de hash na thread principal.
         */
        fun enqueueAutomatic(context: Context) {
            enqueue(context, LocalModelManager.DEFAULT_MODEL_URL, NetworkType.UNMETERED)
        }

        private fun enqueue(context: Context, url: String, networkType: NetworkType) {
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
                .setInputData(workDataOf(KEY_URL to url))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
