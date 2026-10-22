package me.ash.reader.domain.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.model.article.ArticleFts
import me.ash.reader.domain.model.article.hebrewVariants

@Dao
interface ArticleFtsDao {

    @Insert suspend fun insert(articleFts: ArticleFts)

    @Query("DELETE FROM article_fts WHERE articleId = :articleId")
    suspend fun deleteByArticleId(articleId: String)

    /** Article ids are `accountId$remoteId`, so [idPrefix] is `"<accountId>$"`. */
    @Query("DELETE FROM article_fts WHERE substr(articleId, 1, length(:idPrefix)) = :idPrefix")
    suspend fun deleteByAccount(idPrefix: String)

    // FTS4 tables cannot carry a unique constraint, so replace by hand.
    @Transaction
    suspend fun upsert(articleId: String, content: String) {
        deleteByArticleId(articleId)
        insert(ArticleFts(articleId = articleId, content = content, variants = hebrewVariants(content)))
    }

    /** Articles of the account that have no index entry yet, oldest first, a page at a time. */
    @Query(
        """
        SELECT * FROM article WHERE accountId = :accountId
        AND id NOT IN (SELECT articleId FROM article_fts)
        LIMIT :limit
        """
    )
    suspend fun queryUnindexed(accountId: Int, limit: Int): List<Article>

    /** The words around the match in the article's text, for a search result's description. */
    @Query(
        """
        SELECT snippet(article_fts, '', '', '…', 1, 24) FROM article_fts
        WHERE articleId = :articleId AND article_fts MATCH :match LIMIT 1
        """
    )
    suspend fun snippet(articleId: String, match: String): String?

    @Query("SELECT articleId FROM article_fts WHERE articleId IN (:articleIds)")
    suspend fun queryIndexedIds(articleIds: List<String>): List<String>
}
