package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.collapseDuplicates
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalCollapseDuplicates = compositionLocalOf<CollapseDuplicatesPreference> { CollapseDuplicatesPreference.default }

/** Hide every copy of a story republished under another section after the first. */
sealed class CollapseDuplicatesPreference(val value: Boolean) : Preference() {
    data object ON : CollapseDuplicatesPreference(true)
    data object OFF : CollapseDuplicatesPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(collapseDuplicates, value) }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        scope.launch { context.dataStore.put(collapseDuplicates, !value) }

    companion object {

        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[collapseDuplicates]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
