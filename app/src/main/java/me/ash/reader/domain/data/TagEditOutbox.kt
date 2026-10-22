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

/** A read, star or Read Later change for one item that has not reached the server yet. */
@Serializable data class PendingTagEdit(val itemId: String, val tag: String, val add: Boolean)

/**
 * Changes made while the server could not be reached. Without it they were sent once and
 * forgotten, and the next sync, which takes the server's lists as the truth, quietly undid them:
 * an article starred or saved for later on the train was back to how it was by evening.
 *
 * One file per account, in filesDir so it survives the app being killed. Keyed by item and tag, so
 * a later change to the same item replaces an earlier one rather than both being replayed.
 */
@Singleton
class TagEditOutbox @Inject constructor(@ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(accountId: Int) = context.filesDir.resolve("tag_outbox_$accountId.json")

    suspend fun add(accountId: Int, edits: List<PendingTagEdit>) = update(accountId) { pending ->
        val keys = edits.map { it.itemId to it.tag }.toSet()
        pending.filterNot { (it.itemId to it.tag) in keys } + edits
    }

    /** Drops queued changes to these items and tag: a newer change has just reached the server. */
    suspend fun settled(accountId: Int, itemIds: Collection<String>, tag: String) =
        update(accountId) { pending ->
            val ids = itemIds.toSet()
            pending.filterNot { it.tag == tag && it.itemId in ids }
        }

    suspend fun pending(accountId: Int): List<PendingTagEdit> =
        withContext(Dispatchers.IO) { mutex.withLock { read(accountId) } }

    private suspend fun update(
        accountId: Int,
        change: (List<PendingTagEdit>) -> List<PendingTagEdit>,
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = read(accountId)
                val after = change(before)
                if (after == before) return@withLock
                runCatching {
                    if (after.isEmpty()) file(accountId).delete()
                    else file(accountId).writeText(json.encodeToString(after))
                }
            }
        }
    }

    private fun read(accountId: Int): List<PendingTagEdit> =
        runCatching { json.decodeFromString<List<PendingTagEdit>>(file(accountId).readText()) }
            .getOrDefault(emptyList())
}
