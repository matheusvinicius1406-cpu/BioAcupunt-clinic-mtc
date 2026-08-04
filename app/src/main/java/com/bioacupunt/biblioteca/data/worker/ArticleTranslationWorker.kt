package com.bioacupunt.biblioteca.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bioacupunt.biblioteca.domain.model.TranslationLanguage
import com.bioacupunt.biblioteca.domain.usecase.TranslateArticleUseCase
import com.bioacupunt.di.AppContainer
import com.bioacupunt.observability.AppLogger

/**
 * Traduz UM artigo aprovado em background, sobrevivendo à navegação — mesmo raciocínio do
 * [com.bioacupunt.ai.data.provider.ModelDownloadWorker]: uma coroutine presa a
 * `viewModelScope`/composição seria cancelada se a médica saísse da Curadoria no meio da
 * tradução de um artigo longo (várias seções = várias chamadas sequenciais ao modelo local).
 * Resolvido por reflection (WorkerFactory padrão) — mesmo padrão de
 * ModelDownloadWorker/SyncWorker, nenhuma factory precisou ser tocada.
 *
 * Publicação é AUTOMÁTICA assim que a tradução termina (decisão de produto, 2026-08-04: a
 * médica pediu "traduzir e publicar direto") — não há gate de revisão humana aqui. Por isso
 * o resultado nunca é tratado como equivalente ao artigo original aprovado: toda tela que
 * exibe [com.bioacupunt.biblioteca.domain.model.ArticleTranslation] identifica que é
 * tradução automática, não revisada.
 */
class ArticleTranslationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString(KEY_ARTICLE_ID) ?: return Result.failure()
        val language = inputData.getString(KEY_LANGUAGE)?.let(TranslationLanguage::byCode) ?: TranslationLanguage.default

        val repo = AppContainer.articleTranslationRepository
        val article = runCatching { AppContainer.libraryStagingRepository.approvedArticles() }
            .getOrDefault(emptyList())
            .firstOrNull { it.id == articleId }
            ?: return Result.success() // artigo não existe mais / foi desaprovado — nada a fazer

        repo.markProcessing(articleId, language, System.currentTimeMillis())

        return when (val outcome = AppContainer.translateArticleUseCase(article, language)) {
            is TranslateArticleUseCase.Outcome.Success -> {
                repo.markCompleted(
                    articleId, language,
                    title = outcome.title, summary = outcome.summary, content = outcome.content, tags = outcome.tags,
                    now = System.currentTimeMillis(),
                )
                Result.success()
            }
            is TranslateArticleUseCase.Outcome.Error -> {
                AppLogger.w("ArticleTranslationWorker", "Tradução falhou para $articleId: ${outcome.message}")
                repo.markError(articleId, language, outcome.message, System.currentTimeMillis())
                // Result.success(): o worker "terminou" — o resultado definitivo (erro) já
                // está persistido. Retry é uma decisão da médica (botão "Tentar de novo"),
                // não um retry automático do WorkManager sobre uma falha que pode ser
                // estrutural (modelo ausente), não transiente.
                Result.success()
            }
        }
    }

    companion object {
        const val KEY_ARTICLE_ID = "articleId"
        const val KEY_LANGUAGE = "language"

        private fun uniqueWorkName(articleId: String, language: TranslationLanguage) =
            "translate_${articleId}_${language.code}"

        /**
         * Marca PENDING na hora (a médica pode reabrir o artigo um segundo depois e já ver
         * "Na fila", nunca silêncio) e enfileira o worker. Chamado tanto pela Curadoria
         * (logo após aprovar, via `AppContainer.libraryReviewViewModelFactory`) quanto por um
         * botão manual "Traduzir para X" na Biblioteca (cobre o backlog já aprovado antes
         * desta feature existir, e o caso de trocar o idioma de destino em Ajustes).
         *
         * `KEEP`: um segundo toque enquanto a tradução já está rodando não reinicia do zero
         * (mesmo raciocínio do download de modelo) — uma vez que o worker anterior já
         * terminou (sucesso ou erro), um novo enqueue com o mesmo nome inicia normalmente.
         */
        suspend fun enqueue(context: Context, articleId: String, language: TranslationLanguage = TranslationLanguage.default) {
            AppContainer.articleTranslationRepository.markPending(articleId, language, System.currentTimeMillis())
            val request = OneTimeWorkRequestBuilder<ArticleTranslationWorker>()
                .setInputData(workDataOf(KEY_ARTICLE_ID to articleId, KEY_LANGUAGE to language.code))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(uniqueWorkName(articleId, language), ExistingWorkPolicy.KEEP, request)
        }
    }
}
