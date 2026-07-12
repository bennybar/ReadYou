package me.ash.reader.infrastructure.di

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.ash.reader.infrastructure.preference.ImageCacheSizePreference
import me.ash.reader.ui.ext.dataStore
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Provides singleton [ImageLoader] for Coil.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            // Shared OKHttpClient instance.
            .okHttpClient(okHttpClient)
            // This slightly improves scrolling performance
            .dispatcher(Dispatchers.Default)
            .components {
                // Support SVG decoding
                add(SvgDecoder.Factory())
                // Support GIF decoding
                add(
                    if (SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )
            }
            // Enable disk cache
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images"))
                    .maxSizePercent(imageCacheSizePercent(context))
                    .build()
            )
            // Enable memory cache
            .memoryCache(
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            )
            .build()
    }

    // The ImageLoader is a singleton built before any Composable runs, so the preference has to be
    // read straight from DataStore rather than through a CompositionLocal. Changing it takes effect
    // on the next app start.
    private fun imageCacheSizePercent(context: Context): Double = runBlocking {
        ImageCacheSizePreference.fromPreferences(context.dataStore.data.first()).value / 100.0
    }
}
