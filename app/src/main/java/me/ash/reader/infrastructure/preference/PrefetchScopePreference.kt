package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.prefetchScope
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalPrefetchScope =
    compositionLocalOf<PrefetchScopePreference> { PrefetchScopePreference.default }

sealed class PrefetchScopePreference(val value: Int) : Preference() {
    object UnreadOnly : PrefetchScopePreference(0)
    object UnreadAndStarred : PrefetchScopePreference(1)
    object AllArticles : PrefetchScopePreference(2)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.prefetchScope,
                value
            )
        }
    }

    fun toDesc(context: Context): String =
        when (this) {
            UnreadOnly -> context.getString(R.string.prefetch_scope_unread)
            UnreadAndStarred -> context.getString(R.string.prefetch_scope_unread_starred)
            AllArticles -> context.getString(R.string.prefetch_scope_all)
        }

    val includeStarred: Boolean
        get() = this != UnreadOnly

    val includeAll: Boolean
        get() = this == AllArticles

    companion object {

        val default = UnreadOnly
        val values = listOf(UnreadOnly, UnreadAndStarred, AllArticles)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[prefetchScope]?.key as Preferences.Key<Int>]) {
                0 -> UnreadOnly
                1 -> UnreadAndStarred
                2 -> AllArticles
                else -> default
            }
    }
}
