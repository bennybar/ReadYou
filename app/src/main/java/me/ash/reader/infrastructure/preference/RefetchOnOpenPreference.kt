package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.refetchOnOpen
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalRefetchOnOpen = compositionLocalOf<RefetchOnOpenPreference> { RefetchOnOpenPreference.default }

/** Whether opening an article downloads it again from its site, behind the copy on screen. */
sealed class RefetchOnOpenPreference(val value: Int) : Preference() {
    object Off : RefetchOnOpenPreference(0)
    object WiFiOnly : RefetchOnOpenPreference(1)
    object Always : RefetchOnOpenPreference(2)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(DataStoreKey.refetchOnOpen, value) }
    }

    fun toDesc(context: Context): String =
        when (this) {
            Off -> context.getString(R.string.refetch_on_open_off)
            WiFiOnly -> context.getString(R.string.refetch_on_open_wifi)
            Always -> context.getString(R.string.refetch_on_open_always)
        }

    companion object {

        val default = Always
        val values = listOf(Off, WiFiOnly, Always)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[refetchOnOpen]?.key as Preferences.Key<Int>]) {
                0 -> Off
                1 -> WiFiOnly
                2 -> Always
                else -> default
            }
    }
}
