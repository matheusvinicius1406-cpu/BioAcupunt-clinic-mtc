package com.bioacupunt.data.repository

import com.bioacupunt.data.local.database.KnowledgeNodeDao
import com.bioacupunt.data.local.model.KnowledgeNode
import kotlinx.coroutines.flow.Flow

class KnowledgeRepository(
    private val dao: KnowledgeNodeDao
) {
    val allNodes: Flow<List<KnowledgeNode>> = dao.getAllNodes()

    suspend fun refreshNodes() {
        // Lógica para buscar do backend e salvar no Room virá aqui
    }

    fun search(query: String): Flow<List<KnowledgeNode>> = dao.searchNodes(query)
}
