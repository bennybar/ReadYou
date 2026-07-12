package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.removeReadImmediately
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalRemoveReadImmediately =
    compositionLocalOf<RemoveReadImmediatelyPreference> { RemoveReadImmediatelyPreference.default }

/**
 * OFF keeps the existing behaviour: an article marked as read stays in the list, greyed out, until
 * the diffs are flushed. ON drops it from the list as soon as it is read.
 */
sealed class RemoveReadImmediatelyPreference(val value: Boolean) : Preference() {
    data object ON : RemoveReadImmediatelyPreference(true)
    data object OFF : RemoveReadImmediatelyPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                removeReadImmediately,
                value
            )
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) = scope.launch {
        context.dataStore.put(
            removeReadImmediately,
            !value
        )
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (
                preferences[DataStoreKey.keys[removeReadImmediately]?.key as Preferences.Key<Boolean>]
            ) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
