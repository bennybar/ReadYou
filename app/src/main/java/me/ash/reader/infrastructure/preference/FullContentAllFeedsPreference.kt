package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.fullContentAllFeeds
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalFullContentAllFeeds =
    compositionLocalOf<FullContentAllFeedsPreference> { FullContentAllFeedsPreference.default }

sealed class FullContentAllFeedsPreference(val value: Boolean) : Preference() {
    data object ON : FullContentAllFeedsPreference(true)
    data object OFF : FullContentAllFeedsPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                fullContentAllFeeds,
                value
            )
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) = scope.launch {
        context.dataStore.put(
            fullContentAllFeeds,
            !value
        )
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[fullContentAllFeeds]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
