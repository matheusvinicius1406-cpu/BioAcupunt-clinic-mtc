package com.bioacupunt.biblioteca.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleTranslationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ArticleTranslationEntity)

    @Query("SELECT * FROM article_translations WHERE articleId = :articleId AND targetLanguage = :targetLanguage")
    suspend fun getOnce(articleId: String, targetLanguage: String): ArticleTranslationEntity?

    @Query("SELECT * FROM article_translations WHERE articleId = :articleId AND targetLanguage = :targetLanguage")
    fun observe(articleId: String, targetLanguage: String): Flow<ArticleTranslationEntity?>
}
