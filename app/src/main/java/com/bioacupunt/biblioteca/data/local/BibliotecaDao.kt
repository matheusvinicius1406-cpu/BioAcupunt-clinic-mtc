package com.bioacupunt.biblioteca.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bioacupunt.biblioteca.domain.model.BibliotecaNode
import kotlinx.coroutines.flow.Flow

@Dao
interface BibliotecaDao {
    @Query("SELECT * FROM biblioteca_nodes")
    fun observeAll(): Flow<List<BibliotecaNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<BibliotecaNodeEntity>)

    @Query("SELECT * FROM biblioteca_nodes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<BibliotecaNodeEntity>>
}
