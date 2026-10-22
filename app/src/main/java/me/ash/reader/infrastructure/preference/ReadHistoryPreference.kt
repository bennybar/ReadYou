package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.readHistoryMonths
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalReadHistory = compositionLocalOf<ReadHistoryPreference> { ReadHistoryPreference.default }

/**
 * How far back a FreshRSS sync fetches articles that are already read. Unread and starred ones
 * always come, however old; older read ones (Read Later included) only within this window.
 */
sealed class ReadHistoryPreference(val months: Int) : Preference() {
    object OneMonth : ReadHistoryPreference(1)
    object ThreeMonths : ReadHistoryPreference(3)
    object OneYear : ReadHistoryPreference(12)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(DataStoreKey.readHistoryMonths, months) }
    }

    fun toDesc(context: Context): String =
        context.resources.getQuantityString(R.plurals.read_history_months, months, months)

    companion object {

        val default = OneMonth
        val values = listOf(OneMonth, ThreeMonths, OneYear)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[readHistoryMonths]?.key as Preferences.Key<Int>]) {
                1 -> OneMonth
                3 -> ThreeMonths
                12 -> OneYear
                else -> default
            }
    }
}
