package me.ash.reader.domain.service

import javax.inject.Inject
import javax.inject.Singleton
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.repository.ArticleFtsDao
import org.jsoup.Jsoup

/**
 * Keeps the full-text search index in step with the articles. Search only ever looks at this
 * index, so an article missing from it cannot be found at all, not even by its title.
 */
@Singleton
class ArticleIndexer @Inject constructor(private val articleFtsDao: ArticleFtsDao) {

    suspend fun index(article: Article, html: String) {
        val body = if (html.isBlank()) "" else Jsoup.parse(html).text()
        articleFtsDao.upsert(articleId = article.id, content = "${article.title}\n$body")
    }

    /**
     * Indexes every article of the account that is not indexed yet, from its saved full text when
     * there is one and from the feed's own text otherwise. Runs after each sync, so new articles
     * are searchable straight away, whatever the offline-download settings, and the first run
     * after an upgrade backfills the whole archive.
     */
    suspend fun indexNewArticles(accountId: Int, savedFullText: suspend (String) -> String?) {
        while (true) {
            val batch = articleFtsDao.queryUnindexed(accountId, BATCH)
            if (batch.isEmpty()) return
            batch.forEach { index(it, savedFullText(it.id) ?: it.rawDescription) }
        }
    }

    private companion object {
        const val BATCH = 200
    }
}
