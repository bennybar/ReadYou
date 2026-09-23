package me.ash.reader.infrastructure.rss

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.service.AccountService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReaderCacheHelperTest {
    private lateinit var root: File
    private lateinit var rssHelper: RssHelper
    private lateinit var helper: ReaderCacheHelper

    private val article =
        Article(
            id = "1\$abc",
            date = Date(),
            title = "Title",
            rawDescription = "<p>summary</p>",
            shortDescription = "summary",
            link = "https://example.com/a",
            feedId = "1\$feed",
            accountId = 1,
        )

    @Before
    fun setUp() {
        root = Files.createTempDirectory("archive").toFile()
        val context = mock<Context> {
            on { filesDir } doReturn root.resolve("files")
            on { cacheDir } doReturn root.resolve("cache")
        }
        val accountService = mock<AccountService> { on { getCurrentAccountId() } doReturn 1 }
        rssHelper = mock()
        helper = ReaderCacheHelper(context, Dispatchers.Unconfined, rssHelper, accountService)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun accountDir() = root.resolve("files/readability/1")

    private fun cachedFile(suffix: String) =
        accountDir().listFiles()!!.single { it.name.endsWith(suffix) }

    @Test
    fun emptyArchiveFileCountsAsMissing() = runBlocking {
        whenever(rssHelper.parseFullContent(any(), any())) doReturn "<p>full</p>"
        helper.checkOrFetchFullContent(article)
        cachedFile(".html").writeText("")

        assertTrue(helper.readFullContent(article.id).isFailure)
        assertEquals(PrefetchResult.FETCHED, helper.checkOrFetchFullContent(article))
        assertEquals("<p>full</p>", helper.readFullContent(article.id).getOrNull())
    }

    @Test
    fun newBodyClearsTheImageMarker() = runBlocking {
        helper.recordImagePrefetch(article.id, success = true)
        assertTrue(helper.hasPrefetchedImages(article.id))

        whenever(rssHelper.parseFullContent(any(), any())) doReturn "<p>full</p>"
        helper.refetchFullContent(article)

        assertFalse(helper.hasPrefetchedImages(article.id))
        assertFalse(accountDir().listFiles()!!.any { it.name.endsWith(".tmp") })
    }
}
