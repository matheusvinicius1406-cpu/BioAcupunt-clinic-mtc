package com.bioacupunt.biblioteca.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A bookmarked [com.bioacupunt.biblioteca.domain.model.MtcArticle], by its stable string id. */
@Entity(tableName = "favorite_articles", indices = [Index("articleId", unique = true)])
data class FavoriteArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: String,
    val createdAt: String = "",
)

@Dao
interface FavoriteArticleDao {
    @Query("SELECT * FROM favorite_articles")
    fun observeAll(): Flow<List<FavoriteArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: FavoriteArticleEntity)

    @Query("DELETE FROM favorite_articles WHERE articleId = :articleId")
    suspend fun remove(articleId: String)
}
