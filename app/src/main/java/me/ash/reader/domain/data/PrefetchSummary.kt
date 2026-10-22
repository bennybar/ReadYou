package me.ash.reader.domain.data

import android.content.Context
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How far an offline-download run got, and what state it left each article in. */
data class PrefetchSummary(
    val current: Int,
    val total: Int,
    /** Text and every image are on the device. */
    val ready: Int,
    /** Readable offline, but some images are missing. */
    val textOnly: Int,
    val failed: Int,
    /** The run was stopped before it finished — by the system, or by losing Wi-Fi or charge. */
    val interrupted: Boolean = false,
) {
    fun toData(): Data =
        workDataOf(
            CURRENT to current,
            TOTAL to total,
            READY to ready,
            TEXT_ONLY to textOnly,
            FAILED to failed,
        )

    companion object {
        private const val CURRENT = "current"
        private const val TOTAL = "total"
        private const val READY = "ready"
        private const val TEXT_ONLY = "textOnly"
        private const val FAILED = "failed"
        private const val INTERRUPTED = "interrupted"

        fun fromData(data: Data): PrefetchSummary =
            PrefetchSummary(
                current = data.getInt(CURRENT, 0),
                total = data.getInt(TOTAL, 0),
                ready = data.getInt(READY, 0),
                textOnly = data.getInt(TEXT_ONLY, 0),
                failed = data.getInt(FAILED, 0),
            )
    }

    /**
     * Keeps the last finished run, because WorkManager's progress is gone the moment the worker
     * stops — and a run that was retried or interrupted has no output data either.
     */
    @Singleton
    class Store @Inject constructor(@ApplicationContext context: Context) {
        private val prefs = context.getSharedPreferences("prefetch_summary", Context.MODE_PRIVATE)
        private val _last = MutableStateFlow(read())
        val last: StateFlow<PrefetchSummary?> = _last

        fun save(summary: PrefetchSummary?) {
            prefs.edit {
                clear()
                if (summary != null) {
                    putInt(CURRENT, summary.current)
                    putInt(TOTAL, summary.total)
                    putInt(READY, summary.ready)
                    putInt(TEXT_ONLY, summary.textOnly)
                    putInt(FAILED, summary.failed)
                    putBoolean(INTERRUPTED, summary.interrupted)
                }
            }
            _last.value = summary
        }

        private fun read(): PrefetchSummary? {
            if (!prefs.contains(TOTAL)) return null
            return PrefetchSummary(
                current = prefs.getInt(CURRENT, 0),
                total = prefs.getInt(TOTAL, 0),
                ready = prefs.getInt(READY, 0),
                textOnly = prefs.getInt(TEXT_ONLY, 0),
                failed = prefs.getInt(FAILED, 0),
                interrupted = prefs.getBoolean(INTERRUPTED, false),
            )
        }
    }
}
