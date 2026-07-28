package com.bioacupunt.educacao.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Casos aprovados pela médica. O caso fixo fica em código ([com.bioacupunt.educacao.data.BuiltinSimulatedCases]), nunca aqui. */
@Entity(
    tableName = "simulated_cases",
    indices = [Index("tenantId")],
)
data class SimulatedCaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long,
    val title: String,
    val vignette: String,
    /** Perguntas separadas por quebra de linha — texto livre, vírgula quebraria CSV. */
    val questionsText: String,
    val answerKey: String,
    val category: String,
    /** Artigo de origem quando gerado a partir da Curadoria da Biblioteca. Vazio se não veio de artigo. */
    val sourceArticleId: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)
