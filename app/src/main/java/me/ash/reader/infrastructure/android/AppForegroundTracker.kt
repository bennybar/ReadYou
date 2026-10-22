package me.ash.reader.infrastructure.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the app is currently on screen.
 *
 * WorkManager's own tags cannot answer this: a one-time sync is not necessarily user-triggered (the
 * sync-on-start runs one-time work too), and a periodic sync can just as easily fire while the app
 * is open. The only way to say whether a sync ran in the background is to watch the activities.
 */
@Singleton
class AppForegroundTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

    private val startedActivities = AtomicInteger(0)

    val isForeground: Boolean
        get() = startedActivities.get() > 0

    override fun onActivityStarted(activity: Activity) {
        startedActivities.incrementAndGet()
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.decrementAndGet()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
