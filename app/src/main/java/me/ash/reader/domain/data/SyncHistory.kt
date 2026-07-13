package me.ash.reader.domain.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One background sync run, as shown in Settings → Sync log. */
@Serializable
data class SyncRecord(
    /** Epoch millis. Rendered in the device's own time zone when displayed. */
    val startedAt: Long,
    val durationMs: Long,
    val succeeded: Boolean,
    val newArticles: Int,
    /** True when WorkManager ran it as one-time work; false when it fired on the schedule. */
    val manual: Boolean,
    /**
     * Whether the app was on screen while the sync ran. Defaulted so entries written before this
     * field existed still parse; they simply show as background.
     */
    val foreground: Boolean = false,
)

/**
 * A history of sync runs, so it is possible to tell whether background syncing is actually
 * happening. This is separate from [SyncLogger], which only keeps stack traces of syncs that threw.
 */
@Singleton
class SyncHistoryLogger @Inject constructor(@ApplicationContext private val context: Context) {

    // filesDir, not cacheDir: a history that the system can silently evict is not a history.
    private val file = context.filesDir.resolve("sync_history.json")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun record(record: SyncRecord) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val history = (read() + record).takeLast(MAX_ENTRIES)
                runCatching { file.writeText(json.encodeToString(history)) }
            }
        }
    }

    /** Newest first. */
    suspend fun list(): List<SyncRecord> =
        withContext(Dispatchers.IO) { mutex.withLock { read().reversed() } }

    suspend fun clear() {
        withContext(Dispatchers.IO) { mutex.withLock { runCatching { file.delete() } } }
    }

    private fun read(): List<SyncRecord> =
        runCatching { json.decodeFromString<List<SyncRecord>>(file.readText()) }
            .getOrDefault(emptyList())

    companion object {
        private const val MAX_ENTRIES = 100
    }
}
