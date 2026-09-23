package me.ash.reader.ui.page.home.reading

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Where the reader was left in each article, so reopening one carries on from there. Kept for the
 * [CAPACITY] most recently read articles; a position near the top is not worth remembering.
 *
 * Stored as `savedAt;offset` for the WebView renderer (a scroll offset in px) and
 * `savedAt;index;offset` for the native one (a list item and the offset into it).
 */
object ReadingPositions {
    private const val CAPACITY = 300
    private const val MIN_OFFSET = 120

    private fun prefs(context: Context) =
        context.getSharedPreferences("reading_positions", Context.MODE_PRIVATE)

    private fun read(context: Context, articleId: String): List<Int>? =
        prefs(context).getString(articleId, null)?.split(';')?.drop(1)?.map { it.toInt() }

    private fun write(context: Context, articleId: String, position: List<Int>?) {
        val prefs = prefs(context)
        prefs.edit {
            if (position == null) remove(articleId)
            else putString(articleId, (listOf(System.currentTimeMillis()) + position).joinToString(";"))
        }
        val all = prefs.all
        if (all.size > CAPACITY) {
            val oldest =
                all.entries
                    .sortedBy { (it.value as? String)?.substringBefore(';')?.toLongOrNull() ?: 0 }
                    .take(all.size - CAPACITY)
            prefs.edit { oldest.forEach { remove(it.key) } }
        }
    }

    /**
     * Restores the saved position, then keeps it up to date. Restoring waits for the document to
     * grow tall enough: the WebView reports its height after loading and again as images settle,
     * and scrolling past the end of a document that is still short does nothing, silently. Saving
     * only starts once that is settled, so the initial offset of 0 never overwrites the position.
     */
    suspend fun track(context: Context, articleId: String, scrollState: ScrollState) {
        read(context, articleId)?.singleOrNull()?.let { target ->
            withTimeoutOrNull(5_000) { snapshotFlow { scrollState.maxValue }.first { it >= target } }
            // Someone who already started scrolling has chosen where they want to be.
            if (scrollState.value == 0) scrollState.scrollTo(target)
        }
        snapshotFlow { scrollState.value }
            .collectLatest {
                delay(500)
                write(context, articleId, if (it < MIN_OFFSET) null else listOf(it))
            }
    }

    suspend fun track(context: Context, articleId: String, listState: LazyListState) {
        read(context, articleId)?.takeIf { it.size == 2 }?.let { (index, offset) ->
            withTimeoutOrNull(5_000) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > index }
            }
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)
                listState.scrollToItem(index, offset)
        }
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                delay(500)
                write(
                    context,
                    articleId,
                    if (index == 0 && offset < MIN_OFFSET) null else listOf(index, offset),
                )
            }
    }
}
