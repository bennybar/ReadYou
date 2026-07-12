package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.imageCacheSize
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalImageCacheSize =
    compositionLocalOf<ImageCacheSizePreference> { ImageCacheSizePreference.default }

// Percentage of available disk space Coil may use for its image cache.
sealed class ImageCacheSizePreference(val value: Int) : Preference() {
    object Percent2 : ImageCacheSizePreference(2)
    object Percent5 : ImageCacheSizePreference(5)
    object Percent10 : ImageCacheSizePreference(10)
    object Percent25 : ImageCacheSizePreference(25)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.imageCacheSize,
                value
            )
        }
    }

    fun toDesc(context: Context): String =
        context.getString(R.string.image_cache_size_percent, value)

    companion object {

        val default = Percent2
        val values = listOf(Percent2, Percent5, Percent10, Percent25)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[imageCacheSize]?.key as Preferences.Key<Int>]) {
                2 -> Percent2
                5 -> Percent5
                10 -> Percent10
                25 -> Percent25
                else -> default
            }
    }
}
