package me.ash.reader.infrastructure.rss

import android.content.Context
import androidx.annotation.CheckResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.service.AccountService
import me.ash.reader.infrastructure.di.IODispatcher

enum class PrefetchResult {
    /** Was already in the cache; nothing to do. */
    CACHED,
    /** Downloaded successfully during this run, so its search index is now stale. */
    FETCHED,
    /** Fetch failed, but it is worth trying again later. */
    FAILED,
    /** Fetch has failed too many times; treated as permanently dead and not retried. */
    SKIPPED,
}

class ReaderCacheHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val rssHelper: RssHelper,
    private val accountService: AccountService,
) {
    private val cacheDir = context.cacheDir.resolve("readability")

    private val currentCacheDir: File
        get() = cacheDir.resolve(accountService.getCurrentAccountId().toString())

    @OptIn(ExperimentalStdlibApi::class)
    private fun hashOf(articleId: String): String {
        // A single shared MessageDigest is not thread-safe, and the prefetch worker hashes article
        // ids concurrently: interleaved update() calls would produce a wrong hash, so an article
        // could read another article's cache file. Use a fresh instance per call.
        return MessageDigest.getInstance("SHA-256").digest(articleId.toByteArray()).toHexString()
    }

    private fun getFileNameFor(articleId: String): String = hashOf(articleId) + ".html"

    private fun getFailureFileNameFor(articleId: String): String = hashOf(articleId) + ".fail"

    private fun getImageMarkerFileNameFor(articleId: String): String = hashOf(articleId) + ".img"

    private suspend fun writeContentToCache(content: String, articleId: String): Boolean {
        return withContext(ioDispatcher) {
            runCatching {
                    currentCacheDir.run {
                        mkdirs()
                        resolve(getFileNameFor(articleId)).run {
                            createNewFile()
                            writeText(content)
                        }
                    }
                }
                .fold(onSuccess = { true }, onFailure = { false })
        }
    }

    private fun failureCountFor(articleId: String): Int =
        runCatching {
                currentCacheDir.resolve(getFailureFileNameFor(articleId)).readText().trim().toInt()
            }
            .getOrDefault(0)

    private fun recordFailure(articleId: String) {
        runCatching {
            val count = failureCountFor(articleId) + 1
            currentCacheDir.run {
                mkdirs()
                resolve(getFailureFileNameFor(articleId)).writeText(count.toString())
            }
        }
    }

    private fun clearFailure(articleId: String) {
        runCatching { currentCacheDir.resolve(getFailureFileNameFor(articleId)).delete() }
    }

    suspend fun clearAllFailures(): Boolean {
        return withContext(ioDispatcher) {
            runCatching {
                    currentCacheDir
                        .listFiles { file ->
                            file.name.endsWith(".fail") || file.name.endsWith(".img")
                        }
                        ?.forEach { it.delete() }
                    true
                }
                .getOrDefault(false)
        }
    }

    /**
     * Whether this article's images have already been pulled into the image cache.
     *
     * Without this the prefetcher re-read every article off disk, re-parsed its HTML and re-issued
     * every image request on every single sync, forever — which for an archive of a few thousand
     * articles is a genuine drain on the battery for no benefit at all.
     */
    suspend fun hasPrefetchedImages(articleId: String): Boolean {
        return withContext(ioDispatcher) {
            val attempts =
                runCatching {
                        currentCacheDir
                            .resolve(getImageMarkerFileNameFor(articleId))
                            .readText()
                            .trim()
                    }
                    .getOrDefault("")
            attempts == DONE || (attempts.toIntOrNull() ?: 0) >= MAX_FETCH_ATTEMPTS
        }
    }

    /** [success] is false when an image could not be downloaded, so it is worth trying again. */
    suspend fun recordImagePrefetch(articleId: String, success: Boolean) {
        withContext(ioDispatcher) {
            runCatching {
                val marker = currentCacheDir.resolve(getImageMarkerFileNameFor(articleId))
                val contents =
                    if (success) DONE
                    else {
                        val attempts =
                            (marker.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0)
                        (attempts + 1).toString()
                    }
                currentCacheDir.mkdirs()
                marker.writeText(contents)
            }
        }
    }

    @CheckResult
    suspend fun readFullContent(articleId: String): Result<String> {
        return withContext(ioDispatcher) {
            runCatching {
                val file = currentCacheDir.resolve(getFileNameFor(articleId))
                if (!file.exists()) return@withContext Result.failure(FileNotFoundException())
                file.readText()
            }
        }
    }

    private suspend fun fetchFullContentInternal(article: Article): Result<String> {
        return withContext(ioDispatcher) {
            runCatching {
                val fullContent = rssHelper.parseFullContent(article.link, article.title)
                if (fullContent.isNotBlank()) {
                    writeContentToCache(fullContent, article.id)
                    clearFailure(article.id)
                    fullContent
                } else return@withContext Result.failure(Exception())
            }
        }
    }

    /**
     * Downloads the article from its source again, ignoring the cached copy. On failure the
     * existing cache is left untouched, so a refresh that fails does not destroy the copy the
     * reader is currently showing.
     */
    @CheckResult
    suspend fun refetchFullContent(article: Article): Result<String> {
        return withContext(ioDispatcher) { fetchFullContentInternal(article) }
    }

    @CheckResult
    suspend fun readOrFetchFullContent(article: Article): Result<String> {
        return withContext(ioDispatcher) {
            val result = readFullContent(article.id)
            if (result.isSuccess) return@withContext result
            return@withContext fetchFullContentInternal(article)
        }
    }

    suspend fun checkOrFetchFullContent(article: Article): PrefetchResult {
        return withContext(ioDispatcher) {
            try {
                if (currentCacheDir.resolve(getFileNameFor(article.id)).exists()) {
                    return@withContext PrefetchResult.CACHED
                }
                // A dead link (404, paywall, not HTML) fails identically on every sync forever.
                // Once it has burned through its attempts, stop paying for it.
                if (failureCountFor(article.id) >= MAX_FETCH_ATTEMPTS) {
                    return@withContext PrefetchResult.SKIPPED
                }
                fetchFullContentInternal(article)
                    .fold(
                        onSuccess = { PrefetchResult.FETCHED },
                        onFailure = {
                            recordFailure(article.id)
                            PrefetchResult.FAILED
                        },
                    )
            } catch (_: SecurityException) {
                PrefetchResult.FAILED
            }
        }
    }

    suspend fun deleteCacheFor(articleId: String): Boolean {
        return withContext(ioDispatcher) {
            runCatching {
                    clearFailure(articleId)
                    currentCacheDir.resolve(getImageMarkerFileNameFor(articleId)).delete()
                    val file = currentCacheDir.resolve(getFileNameFor(articleId))
                    if (!file.exists()) return@runCatching false
                    return@runCatching file.delete()
                }
                .fold(onSuccess = { it }, onFailure = { false })
        }
    }

    suspend fun clearCache(): Boolean {
        return withContext(ioDispatcher) {
            runCatching {
                    return@withContext currentCacheDir.deleteRecursively()
                }
                .fold(onSuccess = { true }, onFailure = { false })
        }
    }

    companion object {
        private const val MAX_FETCH_ATTEMPTS = 3
        private const val DONE = "done"
    }
}
