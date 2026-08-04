package com.bioacupunt.core.spellcheck

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Fina camada sobre o corretor ortográfico NATIVO do Android (`TextServicesManager`) — não
 * um motor próprio, não IA. Todo aparelho Android já tem um serviço de correção instalado
 * (o mesmo que o teclado usa para sublinhar/sugerir); esta classe só expõe a API assíncrona
 * dele como `suspend fun`, para caber num `LaunchedEffect` debounced.
 *
 * Termos técnicos/MTC nunca chegam até aqui — são filtrados ANTES por [MtcTermsDictionary],
 * em [com.bioacupunt.ui.components.SpellCheckedTextField].
 *
 * Uma [SpellCheckerSession] nova por chamada (nunca reaproveitada entre chamadas): a sessão
 * liga a UM listener fixo na criação — reaproveitar a mesma sessão para uma segunda chamada
 * com um listener novo faria o resultado da segunda chegar no listener (e na continuation)
 * da PRIMEIRA, que já tinha terminado. O custo de recriar a sessão é aceitável porque a
 * chamada já é debounced pelo campo de texto, nunca por tecla.
 */
class SpellCheckService(context: Context) {
    private val manager = context.applicationContext
        .getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager

    data class WordResult(val misspelled: Boolean, val suggestions: List<String>)

    /** Devolve, para cada palavra da lista, se está incorreta e até [MAX_SUGGESTIONS] sugestões. */
    suspend fun check(words: List<String>): Map<String, WordResult> {
        if (words.isEmpty()) return emptyMap()
        val svc = manager ?: return emptyMap()

        return suspendCancellableCoroutine { cont ->
            var session: SpellCheckerSession? = null
            val listener = object : SpellCheckerSessionListener {
                override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                    val map = LinkedHashMap<String, WordResult>()
                    results?.forEachIndexed { index, info ->
                        val word = words.getOrNull(index) ?: return@forEachIndexed
                        val misspelled = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
                        val suggestions = (0 until info.suggestionsCount).mapNotNull { i -> info.getSuggestionAt(i) }
                        map[word] = WordResult(misspelled = misspelled, suggestions = suggestions)
                    }
                    if (cont.isActive) cont.resume(map)
                    session?.close()
                }

                override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) = Unit
            }

            session = runCatching {
                svc.newSpellCheckerSession(null, PT_BR, listener, true)
            }.getOrNull()

            val active = session
            if (active == null) {
                if (cont.isActive) cont.resume(emptyMap())
                return@suspendCancellableCoroutine
            }
            runCatching {
                active.getSuggestions(words.map { TextInfo(it) }.toTypedArray(), MAX_SUGGESTIONS, false)
            }.onFailure {
                if (cont.isActive) cont.resume(emptyMap())
            }
            cont.invokeOnCancellation { active.close() }
        }
    }

    companion object {
        private const val MAX_SUGGESTIONS = 3
        private val PT_BR: Locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    }
}
