package me.ash.reader.domain.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.ash.reader.domain.data.PrefetchSummary
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
    private val prefetchSummaryStore: PrefetchSummary.Store,
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

        // Every article gets indexed for search and scanned for images, not just full-content
        // feeds — the body of a plain feed is just as worth searching. Read Later comes first.
        val articles = rssService.queryPrefetchArticles(allFeeds = true, scope = scope)
        val fullContentIds =
            if (fullContentAllFeeds) null
            else rssService.queryPrefetchArticles(allFeeds = false, scope = scope).map { it.id }.toSet()
        val alreadyIndexed = indexedIds(articles.map { it.id })

        val done = AtomicInteger(0)
        val ready = AtomicInteger(0)
        val textOnly = AtomicInteger(0)
        val failed = AtomicInteger(0)
        fun summary(interrupted: Boolean = false) =
            PrefetchSummary(
                current = done.get(),
                total = articles.size,
                ready = ready.get(),
                textOnly = textOnly.get(),
                failed = failed.get(),
                interrupted = interrupted,
            )
        setProgress(summary().toData())

        try {
            withContext(Dispatchers.IO) {
                articles
                    .map { article ->
                        async {
                            semaphore.withPermit {
                                // Bounded as a whole, not just per network read: only two run at
                                // a time, so an article that never finishes does not merely delay
                                // itself, it takes a worker away for good.
                                val outcome =
                                    withTimeoutOrNull(ARTICLE_TIMEOUT_MS) {
                                        processOne(
                                            article = article,
                                            fullContent = fullContentIds?.contains(article.id) ?: true,
                                            alreadyIndexed = article.id in alreadyIndexed,
                                            prefetchImages = prefetchImages,
                                        )
                                    }
                                        ?: Outcome.FAILED.also {
                                            cacheHelper.recordFailure(article.id)
                                            retryableFailures.incrementAndGet()
                                        }
                                when (outcome) {
                                    Outcome.READY -> ready
                                    Outcome.TEXT_ONLY -> textOnly
                                    Outcome.FAILED -> failed
                                }.incrementAndGet()
                                done.incrementAndGet()
                                setProgress(summary().toData())
                            }
                        }
                    }
                    .awaitAll()
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) { prefetchSummaryStore.save(summary(interrupted = true)) }
            throw e
        }
        prefetchSummaryStore.save(summary())

        // Only retryable failures are worth another pass. Articles written off as dead still show
        // as failed but don't count toward a retry, so one broken link can no longer retry forever — which would also
        // have stalled the WidgetUpdateWorker chained after this one.
        return if (retryableFailures.get() > 0 && runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry()
        else Result.success()
    }

    private val retryableFailures = AtomicInteger(0)

    private enum class Outcome {
        /** Text and every image are on the device. */
        READY,
        /** Readable offline, but some images are missing. */
        TEXT_ONLY,
        /** The full text could not be fetched. */
        FAILED,
    }

    /** Finishes text, search index and images for one article before the next one starts. */
    private suspend fun processOne(
        article: Article,
        fullContent: Boolean,
        alreadyIndexed: Boolean,
        prefetchImages: Boolean,
    ): Outcome {
        val fetch = if (fullContent) cacheHelper.checkOrFetchFullContent(article) else null
        if (fetch == PrefetchResult.FAILED) retryableFailures.incrementAndGet()

        // An article fetched just now was indexed (if at all) from the feed's summary, so its
        // index entry has to be rebuilt from the full text.
        val needsIndexing = fetch == PrefetchResult.FETCHED || !alreadyIndexed
        val needsImages = prefetchImages && !cacheHelper.hasPrefetchedImages(article.id)

        // An article that is already indexed and whose images are already cached costs nothing
        // on later runs: no disk read, no HTML parse, no image requests.
        var imagesCached = !prefetchImages || !needsImages
        if (needsIndexing || needsImages) {
            val html = cacheHelper.readFullContent(article.id).getOrNull() ?: article.rawDescription
            if (needsIndexing) indexForSearch(article, html)
            if (needsImages) {
                imagesCached = prefetchImagesFor(article, html)
                cacheHelper.recordImagePrefetch(articleId = article.id, success = imagesCached)
            }
        }

        return when {
            fetch == PrefetchResult.FAILED || fetch == PrefetchResult.SKIPPED -> Outcome.FAILED
            imagesCached -> Outcome.READY
            else -> Outcome.TEXT_ONLY
        }
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
        private const val ARTICLE_TIMEOUT_MS = 60_000L
        private const val SQLITE_VARIABLE_LIMIT = 900
        private const val READER_ONETIME_NAME = "READER_ONETIME"

        const val RETRY_FAILED = "retryFailed"

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
