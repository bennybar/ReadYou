package me.ash.reader.infrastructure.android

import android.content.Context
import com.aptabase.Aptabase

/**
 * Counts daily users through Aptabase. One event per app open, carrying nothing beyond what the
 * SDK attaches itself (app version, OS, locale); no article, feed or account data.
 */
object UsageAnalytics {
    private const val APP_KEY = "A-US-7547760327"

    fun initialize(context: Context) {
        Aptabase.instance.initialize(context.applicationContext, APP_KEY)
    }

    fun trackAppOpened() {
        Aptabase.instance.trackEvent("app_started")
    }
}
