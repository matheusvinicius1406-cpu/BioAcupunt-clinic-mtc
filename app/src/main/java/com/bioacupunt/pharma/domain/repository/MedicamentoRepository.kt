package com.bioacupunt.pharma.domain.repository

import com.bioacupunt.pharma.domain.model.Medicamento

/** Catálogo ANVISA — somente leitura em runtime, populado uma vez no boot. */
interface MedicamentoRepository {
    suspend fun getById(id: String): Medicamento?
    suspend fun getByIds(ids: List<String>): List<Medicamento>

    /** Busca tolerante (prefixo por termo) via FTS4. Vazio se a query for vazia/inválida. */
    suspend fun search(query: String, limit: Int = 30): List<Medicamento>

    suspend fun count(): Int

    /** Idempotente: só insere se a tabela estiver vazia (ver `AppContainer.seedPharmaCatalogIfNeeded`). */
    suspend fun seedIfEmpty(items: List<Medicamento>)
}
