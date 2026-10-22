package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.expressiveStyle
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalExpressiveStyle = compositionLocalOf<ExpressiveStylePreference> { ExpressiveStylePreference.default }

/**
 * The Material 3 Expressive look: larger type, tonal cards with varied shapes, connected button
 * groups and floating toolbars. OFF keeps the original ReadYou screens.
 */
sealed class ExpressiveStylePreference(val value: Boolean) : Preference() {
    data object ON : ExpressiveStylePreference(true)
    data object OFF : ExpressiveStylePreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(expressiveStyle, value) }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        scope.launch { context.dataStore.put(expressiveStyle, !value) }

    companion object {

        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[expressiveStyle]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
