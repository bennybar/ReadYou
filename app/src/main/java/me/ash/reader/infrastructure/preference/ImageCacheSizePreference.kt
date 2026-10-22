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

// Gigabytes the offline image archive may use. An absolute size, because Coil clamps a percentage
// of the disk to 250 MB, which a large archive outgrows quickly. The stored numbers 2/5/10/25 used
// to be percentages and now read as gigabytes.
sealed class ImageCacheSizePreference(val value: Int) : Preference() {
    object Gb1 : ImageCacheSizePreference(1)
    object Gb2 : ImageCacheSizePreference(2)
    object Gb5 : ImageCacheSizePreference(5)
    object Gb10 : ImageCacheSizePreference(10)
    object Gb25 : ImageCacheSizePreference(25)

    val bytes: Long
        get() = value * 1024L * 1024 * 1024

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.imageCacheSize,
                value
            )
        }
    }

    fun toDesc(context: Context): String =
        context.getString(R.string.image_cache_size_gb, value)

    companion object {

        val default = Gb2
        val values = listOf(Gb1, Gb2, Gb5, Gb10, Gb25)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[imageCacheSize]?.key as Preferences.Key<Int>]) {
                1 -> Gb1
                2 -> Gb2
                5 -> Gb5
                10 -> Gb10
                25 -> Gb25
                else -> default
            }
    }
}
