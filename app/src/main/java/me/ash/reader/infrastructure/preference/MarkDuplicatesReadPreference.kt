package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.markDuplicatesRead
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalMarkDuplicatesRead = compositionLocalOf<MarkDuplicatesReadPreference> { MarkDuplicatesReadPreference.default }

/** Mark the hidden copies of a story read, on the server too, so they do not inflate its unread count. */
sealed class MarkDuplicatesReadPreference(val value: Boolean) : Preference() {
    data object ON : MarkDuplicatesReadPreference(true)
    data object OFF : MarkDuplicatesReadPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(markDuplicatesRead, value) }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        scope.launch { context.dataStore.put(markDuplicatesRead, !value) }

    companion object {

        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[markDuplicatesRead]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
