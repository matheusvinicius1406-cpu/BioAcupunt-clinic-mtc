package com.bioacupunt.pharma.data.repository

import com.bioacupunt.pharma.data.local.MedicamentoDao
import com.bioacupunt.pharma.data.local.MedicamentoFtsDao
import com.bioacupunt.pharma.data.local.toDomain
import com.bioacupunt.pharma.data.local.toEntity
import com.bioacupunt.pharma.data.local.toFtsEntity
import com.bioacupunt.pharma.domain.model.ClasseTerapeuticaSummary
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.repository.MedicamentoRepository

class MedicamentoRepositoryImpl(
    private val dao: MedicamentoDao,
    private val ftsDao: MedicamentoFtsDao,
) : MedicamentoRepository {

    override suspend fun getById(id: String): Medicamento? = dao.getById(id)?.toDomain()

    override suspend fun getByIds(ids: List<String>): List<Medicamento> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids).map { it.toDomain() }

    override suspend fun search(query: String, limit: Int): List<Medicamento> {
        val ftsQuery = buildFtsQuery(query) ?: return emptyList()
        val hits = runCatching { ftsDao.search(ftsQuery, limit) }.getOrDefault(emptyList())
        if (hits.isEmpty()) return emptyList()

        val orderedIds = hits.map { it.medicamentoId }
        val byId = dao.getByIds(orderedIds).associateBy { it.id }
        return orderedIds.mapNotNull { byId[it]?.toDomain() }
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun seedIfEmpty(items: List<Medicamento>) {
        if (dao.count() > 0) return
        dao.insertAll(items.map { it.toEntity() })
        ftsDao.insertAll(items.map { it.toFtsEntity() })
    }

    override suspend fun listClasses(): List<ClasseTerapeuticaSummary> =
        dao.listClasses().map { ClasseTerapeuticaSummary(it.classeTerapeutica, it.count) }

    override suspend fun getByClasse(classe: String, limit: Int): List<Medicamento> =
        dao.getByClasse(classe, limit).map { it.toDomain() }
}

/**
 * Um termo por prefixo (`palavra*`), unidos por AND implícito do FTS4 — tolerante a
 * digitação incompleta sem exigir match exato. Caracteres fora de letra/dígito são
 * removidos: MATCH do FTS4 trata `-`, `"`, `:` como sintaxe, não como texto, e um termo
 * cru vindo direto do campo de busca pode derrubar a query inteira com `SQLiteException`.
 */
private fun buildFtsQuery(raw: String): String? {
    val terms = raw.trim()
        .split(Regex("\\s+"))
        .map { term -> term.filter { it.isLetterOrDigit() } }
        .filter { it.isNotBlank() }
    if (terms.isEmpty()) return null
    return terms.joinToString(" ") { "$it*" }
}
