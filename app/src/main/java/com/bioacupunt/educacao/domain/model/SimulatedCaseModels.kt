package com.bioacupunt.educacao.domain.model

/**
 * Um caso clínico simulado do Simulador, seja o único fixo em código ([builtin] = true,
 * read-only, movido verbatim de `SimuladorScreen.kt`) ou um aprovado pela médica na
 * Curadoria da Biblioteca (tabela `simulated_cases`, [userRowId] aponta a linha).
 *
 * Educacional por natureza — nenhum campo aqui referencia paciente real; é conteúdo de
 * estudo, não prontuário. [sourceArticleId] rastreia de qual artigo da Biblioteca o caso
 * foi derivado, quando aplicável.
 */
data class SimulatedCase(
    val key: String,
    val title: String,
    val vignette: String,
    val questions: List<String>,
    val answerKey: String,
    val category: String,
    val builtin: Boolean,
    val sourceArticleId: String = "",
    val userRowId: Long? = null,
)
