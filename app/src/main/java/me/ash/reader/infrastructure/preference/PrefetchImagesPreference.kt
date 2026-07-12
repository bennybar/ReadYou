package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.prefetchImages
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalPrefetchImages =
    compositionLocalOf<PrefetchImagesPreference> { PrefetchImagesPreference.default }

sealed class PrefetchImagesPreference(val value: Boolean) : Preference() {
    data object ON : PrefetchImagesPreference(true)
    data object OFF : PrefetchImagesPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                prefetchImages,
                value
            )
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) = scope.launch {
        context.dataStore.put(
            prefetchImages,
            !value
        )
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[prefetchImages]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}
