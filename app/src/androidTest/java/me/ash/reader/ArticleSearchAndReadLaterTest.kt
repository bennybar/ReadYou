package me.ash.reader

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Date
import kotlinx.coroutines.runBlocking
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.model.article.ArticleWithFeed
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.group.Group
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.repository.ArticleFtsDao
import me.ash.reader.domain.repository.FeedDao
import me.ash.reader.domain.repository.GroupDao
import me.ash.reader.infrastructure.db.AndroidDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val ACCOUNT_ID = 1

@RunWith(AndroidJUnit4::class)
class ArticleSearchAndReadLaterTest {

    private lateinit var db: AndroidDatabase
    private lateinit var articleDao: ArticleDao
    private lateinit var articleFtsDao: ArticleFtsDao
    private lateinit var feedDao: FeedDao
    private lateinit var groupDao: GroupDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AndroidDatabase::class.java).build()
        articleDao = db.articleDao()
        articleFtsDao = db.articleFtsDao()
        feedDao = db.feedDao()
        groupDao = db.groupDao()

        groupDao.insert(Group(id = "g1", name = "Group", accountId = ACCOUNT_ID))
        feedDao.insert(
            Feed(
                id = "f1",
                name = "Feed",
                url = "https://example.com/rss",
                groupId = "g1",
                accountId = ACCOUNT_ID,
            )
        )
    }

    @After fun tearDown() = db.close()

    private fun article(id: String, title: String) =
        Article(
            id = id,
            date = Date(),
            title = title,
            rawDescription = "",
            shortDescription = "",
            link = "https://example.com/$id",
            feedId = "f1",
            accountId = ACCOUNT_ID,
        )

    private suspend fun load(
        source: PagingSource<Int, ArticleWithFeed>
    ): List<ArticleWithFeed> {
        val page =
            source.load(
                PagingSource.LoadParams.Refresh(null, 20, false)
            ) as PagingSource.LoadResult.Page
        return page.data
    }

    /**
     * The whole point of the FTS index: a word that appears only in the article body, and nowhere
     * in the title or the 280-character shortDescription preview, must still be findable.
     */
    @Test
    fun searchFindsWordThatAppearsOnlyInTheBody() = runBlocking {
        articleDao.insert(article("a1", "An unremarkable headline"))
        articleDao.insert(article("a2", "Another headline"))

        articleFtsDao.upsert(
            articleId = "a1",
            content = "An unremarkable headline\nThe borrow checker rejects use-after-free.",
        )
        articleFtsDao.upsert(
            articleId = "a2",
            content = "Another headline\nTomatoes need full sun.",
        )

        // "borrow" exists only in a1's body.
        val hits = load(articleDao.searchArticleWhenAll(ACCOUNT_ID, "borrow*"))
        assertEquals(1, hits.size)
        assertEquals("a1", hits.first().article.id)

        // A term from the other article must not leak in.
        val none = load(articleDao.searchArticleWhenAll(ACCOUNT_ID, "borrow tomatoes*"))
        assertTrue(none.isEmpty())
    }

    /** Re-indexing an article must replace its old text, since FTS4 has no unique constraint. */
    @Test
    fun reindexingReplacesPreviousContent() = runBlocking {
        articleDao.insert(article("a1", "Headline"))
        articleFtsDao.upsert(articleId = "a1", content = "Headline\nprovisional summary text")
        articleFtsDao.upsert(articleId = "a1", content = "Headline\nthe full downloaded article")

        assertTrue(load(articleDao.searchArticleWhenAll(ACCOUNT_ID, "downloaded*")).isNotEmpty())
        // The stale summary must be gone, not merely shadowed by a second row.
        assertTrue(load(articleDao.searchArticleWhenAll(ACCOUNT_ID, "provisional*")).isEmpty())
        assertEquals(listOf("a1"), articleFtsDao.queryIndexedIds(listOf("a1")))
    }

    @Test
    fun readLaterFlagRoundTrips() = runBlocking {
        articleDao.insert(article("a1", "Keep this"))
        articleDao.insert(article("a2", "Not this"))

        assertTrue(load(articleDao.queryArticleWithFeedWhenIsReadLater(ACCOUNT_ID, true)).isEmpty())

        articleDao.markAsReadLaterByArticleId(ACCOUNT_ID, "a1", true)

        val marked = load(articleDao.queryArticleWithFeedWhenIsReadLater(ACCOUNT_ID, true))
        assertEquals(1, marked.size)
        assertEquals("a1", marked.first().article.id)
        assertEquals(listOf("a1"), articleDao.queryReadLaterArticleIds(ACCOUNT_ID))

        // Unmarking is what the FreshRSS pull does when the label is removed on another device.
        articleDao.markAsReadLaterByIdSet(ACCOUNT_ID, setOf("a1"), false)
        assertTrue(articleDao.queryReadLaterArticleIds(ACCOUNT_ID).isEmpty())
    }
}
