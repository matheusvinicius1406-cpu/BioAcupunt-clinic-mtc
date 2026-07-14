package com.bioacupunt.biblioteca.domain.usecase

import com.bioacupunt.biblioteca.data.local.FavoriteArticleDao
import com.bioacupunt.biblioteca.data.local.FavoriteArticleEntity
import java.time.Instant

/** Bookmarks/unbookmarks a real library article by its stable id. */
class ToggleFavoriteArticle(private val dao: FavoriteArticleDao) {
    suspend operator fun invoke(articleId: String, makeFavorite: Boolean) {
        if (makeFavorite) {
            dao.add(FavoriteArticleEntity(articleId = articleId, createdAt = Instant.now().toString()))
        } else {
            dao.remove(articleId)
        }
    }
}
