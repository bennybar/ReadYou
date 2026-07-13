package me.ash.reader

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.ash.reader.infrastructure.android.AppForegroundTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sync log splits runs into "background" and "while the app was open" purely on what this
 * reports, so it has to be right in both directions.
 */
@RunWith(AndroidJUnit4::class)
class AppForegroundTrackerTest {

    private val tracker = AppForegroundTracker()
    private lateinit var activity: Activity
    private lateinit var other: Activity

    @Before
    fun setUp() {
        // Activity's constructor builds a Handler, so it can only be created on a Looper thread.
        // The tracker never touches the instances; it only counts them.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = Activity()
            other = Activity()
        }
    }

    @Test
    fun reportsBackgroundBeforeAnyActivityStarts() {
        assertFalse(tracker.isForeground)
    }

    @Test
    fun reportsForegroundWhileAnActivityIsStarted() {
        tracker.onActivityStarted(activity)
        assertTrue(tracker.isForeground)
    }

    @Test
    fun returnsToBackgroundOnceTheActivityStops() {
        tracker.onActivityStarted(activity)
        tracker.onActivityStopped(activity)
        assertFalse(tracker.isForeground)
    }

    /** A rotation stops one activity while another has already started; that is not backgrounding. */
    @Test
    fun staysForegroundWhileAnyActivityRemainsStarted() {
        tracker.onActivityStarted(activity)
        tracker.onActivityStarted(other)
        tracker.onActivityStopped(activity)
        assertTrue(tracker.isForeground)

        tracker.onActivityStopped(other)
        assertFalse(tracker.isForeground)
    }
}
