package com.bioacupunt.data.local.database

import androidx.room.*
import com.bioacupunt.data.local.model.KnowledgeNode
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeNodeDao {
    @Query("SELECT * FROM knowledge_nodes")
    fun getAllNodes(): Flow<List<KnowledgeNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<KnowledgeNode>)

    @Query("SELECT * FROM knowledge_nodes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNodes(query: String): Flow<List<KnowledgeNode>>
}
