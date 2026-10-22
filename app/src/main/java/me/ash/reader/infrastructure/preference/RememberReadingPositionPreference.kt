package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.rememberReadingPosition
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalRememberReadingPosition =
    compositionLocalOf<RememberReadingPositionPreference> { RememberReadingPositionPreference.default }

/** ON reopens an article where you stopped reading it; OFF always opens articles at the top. */
sealed class RememberReadingPositionPreference(val value: Boolean) : Preference() {
    data object ON : RememberReadingPositionPreference(true)
    data object OFF : RememberReadingPositionPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(rememberReadingPosition, value) }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        scope.launch { context.dataStore.put(rememberReadingPosition, !value) }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (
                preferences[DataStoreKey.keys[rememberReadingPosition]?.key as Preferences.Key<Boolean>]
            ) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
