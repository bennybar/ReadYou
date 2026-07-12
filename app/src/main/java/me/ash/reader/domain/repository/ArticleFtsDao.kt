package me.ash.reader.domain.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.ash.reader.domain.model.article.ArticleFts

@Dao
interface ArticleFtsDao {

    @Insert suspend fun insert(articleFts: ArticleFts)

    @Query("DELETE FROM article_fts WHERE articleId = :articleId")
    suspend fun deleteByArticleId(articleId: String)

    // FTS4 tables cannot carry a unique constraint, so replace by hand.
    @Transaction
    suspend fun upsert(articleId: String, content: String) {
        deleteByArticleId(articleId)
        insert(ArticleFts(articleId = articleId, content = content))
    }

    @Query("SELECT articleId FROM article_fts WHERE articleId IN (:articleIds)")
    suspend fun queryIndexedIds(articleIds: List<String>): List<String>
}
