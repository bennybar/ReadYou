package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.unreadBadge
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalUnreadBadge = compositionLocalOf<UnreadBadgePreference> { UnreadBadgePreference.default }

/**
 * Android has no way to write a number onto a launcher icon. A badge only exists for as long as a
 * notification is alive to carry it, so turning this on keeps one silent notification posted with
 * the unread count on it.
 */
sealed class UnreadBadgePreference(val value: Boolean) : Preference() {
    data object ON : UnreadBadgePreference(true)
    data object OFF : UnreadBadgePreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                unreadBadge,
                value
            )
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) = scope.launch {
        context.dataStore.put(
            unreadBadge,
            !value
        )
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[unreadBadge]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
