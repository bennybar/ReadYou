package me.ash.reader.infrastructure.android

import android.content.Context

/** This build reports no usage; only the GitHub build counts launches. */
object UsageAnalytics {
    fun initialize(context: Context) {}

    fun trackAppOpened() {}
}
