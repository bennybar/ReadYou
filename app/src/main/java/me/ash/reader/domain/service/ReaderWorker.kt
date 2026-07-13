package me.ash.reader.domain.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.repository.ArticleFtsDao
import me.ash.reader.infrastructure.preference.toSettings
import me.ash.reader.infrastructure.rss.PrefetchResult
import me.ash.reader.infrastructure.rss.ReaderCacheHelper
import me.ash.reader.ui.ext.dataStore
import org.jsoup.Jsoup

@HiltWorker
class ReaderWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rssService: RssService,
    private val cacheHelper: ReaderCacheHelper,
    private val articleFtsDao: ArticleFtsDao,
    private val imageLoader: ImageLoader,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = applicationContext.dataStore.data.first().toSettings()
        val fullContentAllFeeds = settings.fullContentAllFeeds.value
        val prefetchImages = settings.prefetchImages.value
        val scope = settings.prefetchScope

        // A manual "Download now" is the user explicitly asking us to try again, so give the
        // links we had written off another chance.
        if (inputData.getBoolean(RETRY_FAILED, false)) {
            cacheHelper.clearAllFailures()
        }

        val semaphore = Semaphore(2)
        val rssService = rssService.get()

        val fullContentArticles =
            rssService.queryPrefetchArticles(allFeeds = fullContentAllFeeds, scope = scope)
        // Every article gets indexed for search and scanned for images, not just full-content
        // feeds — the body of a plain feed is just as worth searching.
        val allArticles = rssService.queryPrefetchArticles(allFeeds = true, scope = scope)

        val total = fullContentArticles.size + allArticles.size
        val done = AtomicInteger(0)
        setProgress(progressData(0, total))

        val results =
            withContext(Dispatchers.IO) {
                fullContentArticles
                    .map { article ->
                        async {
                            semaphore.withPermit {
                                val result = cacheHelper.checkOrFetchFullContent(article)
                                setProgress(progressData(done.incrementAndGet(), total))
                                article.id to result
                            }
                        }
                    }
                    .awaitAll()
            }

        val failureCount = results.count { it.second == PrefetchResult.FAILED }
        // Articles fetched just now were indexed (if at all) from the feed's summary, so their
        // index entry has to be rebuilt from the full text we just downloaded.
        val freshlyFetched =
            results.filter { it.second == PrefetchResult.FETCHED }.map { it.first }.toSet()
        val alreadyIndexed = indexedIds(allArticles.map { it.id })

        withContext(Dispatchers.IO) {
            allArticles
                .map { article ->
                    async {
                        semaphore.withPermit {
                            val needsIndexing =
                                article.id in freshlyFetched || article.id !in alreadyIndexed
                            val needsImages =
                                prefetchImages && !cacheHelper.hasPrefetchedImages(article.id)

                            // An article that is already indexed and whose images are already
                            // cached costs nothing on later runs. Without this the worker re-read
                            // every article off disk, re-parsed its HTML and re-issued every image
                            // request on every sync, forever — pointless work, and on an archive of
                            // a few thousand articles a real drain on the battery.
                            if (needsIndexing || needsImages) {
                                val html =
                                    cacheHelper.readFullContent(article.id).getOrNull()
                                        ?: article.rawDescription

                                if (needsIndexing) {
                                    indexForSearch(article, html)
                                }
                                if (needsImages) {
                                    cacheHelper.recordImagePrefetch(
                                        articleId = article.id,
                                        success = prefetchImagesFor(article, html),
                                    )
                                }
                            }
                            setProgress(progressData(done.incrementAndGet(), total))
                        }
                    }
                }
                .awaitAll()
        }

        // Only retryable failures are worth another pass. Articles written off as dead come back
        // as SKIPPED and are excluded, so one broken link can no longer retry forever — which
        // would also have stalled the WidgetUpdateWorker chained after this one.
        return if (failureCount > 0 && runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry()
        else Result.success()
    }

    private suspend fun indexedIds(articleIds: List<String>): Set<String> =
        // SQLite caps the number of bound variables per statement, so an "All articles" archive
        // would blow past it in a single IN (...) query.
        articleIds
            .chunked(SQLITE_VARIABLE_LIMIT)
            .flatMap { articleFtsDao.queryIndexedIds(it) }
            .toSet()

    private suspend fun indexForSearch(article: Article, html: String) {
        val body = if (html.isBlank()) "" else Jsoup.parse(html).text()
        articleFtsDao.upsert(articleId = article.id, content = "${article.title}\n$body")
    }

    private fun progressData(current: Int, total: Int): Data =
        workDataOf(PROGRESS_CURRENT to current, PROGRESS_TOTAL to total)

    /** @return true when every image was cached, so the article never has to be scanned again. */
    private suspend fun prefetchImagesFor(article: Article, html: String): Boolean {
        if (html.isBlank()) return true

        val urls =
            Jsoup.parse(html, article.link)
                .select("img[src]")
                .map { it.absUrl("src") }
                .filter { it.startsWith("http") }
                .distinct()

        return urls.all { url ->
            // The disk cache stores the original bytes whatever size we decode at, and nothing is
            // on screen, so decode at 1x1 to keep large images from blowing up memory here.
            val result =
                imageLoader.execute(
                    ImageRequest.Builder(applicationContext)
                        .data(url)
                        .size(1, 1)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build()
                )
            result is SuccessResult
        }
    }

    companion object {
        private const val MAX_RUN_ATTEMPTS = 3
        private const val SQLITE_VARIABLE_LIMIT = 900
        private const val READER_ONETIME_NAME = "READER_ONETIME"

        const val RETRY_FAILED = "retryFailed"
        const val PROGRESS_CURRENT = "progressCurrent"
        const val PROGRESS_TOTAL = "progressTotal"

        fun enqueueOneTimeWork(workManager: WorkManager) {
            workManager.enqueueUniqueWork(
                READER_ONETIME_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReaderWorker>()
                    .addTag(SyncWorker.READER_TAG)
                    .addTag(SyncWorker.ONETIME_WORK_TAG)
                    .setInputData(workDataOf(RETRY_FAILED to true))
                    .build(),
            )
        }
    }
}
