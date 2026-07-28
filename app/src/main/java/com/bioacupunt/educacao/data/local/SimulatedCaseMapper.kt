package com.bioacupunt.educacao.data.local

import com.bioacupunt.educacao.domain.model.SimulatedCase

fun SimulatedCaseEntity.toDomain(): SimulatedCase = SimulatedCase(
    key = "user_$id",
    title = title,
    vignette = vignette,
    questions = questionsText.split("\n").map { it.trim() }.filter { it.isNotBlank() },
    answerKey = answerKey,
    category = category,
    builtin = false,
    sourceArticleId = sourceArticleId,
    userRowId = id,
)

/** Só para casos da médica ([SimulatedCase.builtin] = false) — o repositório recusa builtin antes de chamar isto. */
fun SimulatedCase.toEntity(tenantId: Long, now: String): SimulatedCaseEntity = SimulatedCaseEntity(
    id = userRowId ?: 0L,
    tenantId = tenantId,
    title = title,
    vignette = vignette,
    questionsText = questions.joinToString("\n"),
    answerKey = answerKey,
    category = category,
    sourceArticleId = sourceArticleId,
    createdAt = now,
    updatedAt = now,
)
